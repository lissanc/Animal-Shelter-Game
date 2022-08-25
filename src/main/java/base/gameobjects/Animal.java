package base.gameobjects;

import base.Game;
import base.gameobjects.animalstates.*;
import base.gameobjects.interactionzones.InteractionZonePetHeart;
import base.graphicsservice.*;
import base.gui.HeartIcon;
import base.map.GameMap;
import base.navigationservice.Direction;
import base.navigationservice.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static base.constants.ColorConstant.GREEN;
import static base.constants.ColorConstant.YELLOW;
import static base.constants.Constants.*;
import static base.constants.FilePath.IMAGES_PATH;
import static base.gameobjects.AgeStage.ADULT;
import static base.gameobjects.AgeStage.BABY;
import static base.navigationservice.Direction.*;

public abstract class Animal implements GameObject, Walking {

    private Sprite previewSprite;
    private AnimatedSprite animatedSprite;
    private final Rectangle animalRectangle;
    private final int tileSize;
    private String fileName;

    private Direction direction;
    private transient Route route;

    private AnimalState state;
    private WalkingState walkingState = new WalkingState();
    private WaitingState waitingState = new WaitingState();
    private EatingState eatingState = new EatingState();
    private SleepingState sleepingState = new SleepingState();
    private FallingAsleepState fallingAsleepState = new FallingAsleepState();
    private WakingUpState wakingUpState = new WakingUpState();

    private String currentMap;
    private int speed;
    private String color;
    private String animalType;
    private String name;
    private boolean favorite;

    private AgeStage age;
    public static final int GROWING_UP_TIME = 200_000;
    private int currentAge;

    public static final int MAX_HUNGER = 30_000;
    private int currentHunger;

    public static final int MAX_THIRST = 25_000;
    private int currentThirst;

    public static final int MAX_ENERGY = 40_000;
    protected static final int MIN_ENERGY = 1;
    private int currentEnergy;

    private final InteractionZonePetHeart interactionZone;
    private final HeartIcon heartIcon;

    protected static final Logger logger = LoggerFactory.getLogger(Animal.class);

    protected Animal(String animalType, int startX, int startY, int tileSize) {
        this(animalType, startX, startY, 1, tileSize, MAX_HUNGER, MAX_THIRST, MAX_ENERGY, ADULT, "");
    }

    protected Animal(String animalType, int startX, int startY, int speed, int tileSize, int currentHunger, int currentThirst, int currentEnergy, AgeStage age, String name) {
        this.animalType = animalType;
        this.tileSize = tileSize;
        this.speed = speed;
        this.currentHunger = currentHunger;
        this.currentThirst = currentThirst;
        this.currentEnergy = currentEnergy;
        this.age = age;
        this.name = name;

        loadAnimatedSprite();
        setPreviewSprite();

        direction = DOWN;
        updateDirection();
        animalRectangle = new Rectangle(startX, startY, 32, 32);
        animalRectangle.generateBorder(1, GREEN);

        if (BABY.equals(age)) {
            this.speed--;
        } else {
            setCurrentAge(GROWING_UP_TIME);
        }

        route = new Route();
        interactionZone = new InteractionZonePetHeart(animalRectangle.getX() + 32, animalRectangle.getY() + 32, 50);
        heartIcon = new HeartIcon();
        state = waitingState;
    }

    protected void loadAnimatedSprite() {
        animatedSprite = ImageLoader.getAnimatedSprite(IMAGES_PATH + animalType + ".png", tileSize);
    }

    protected void setPreviewSprite() {
        previewSprite = ImageLoader.getPreviewSprite(IMAGES_PATH + animalType + "-preview.png");
    }

    public void updateDirection() {
        int startSprite = getStartingSprite(direction);
        int endSprite = getEndSprite(direction);
        animatedSprite.setAnimationRange(startSprite, endSprite);
    }

    protected int getStartingSprite(Direction direction) {
        if (animatedSprite != null && direction != STAY) {
            if (direction.name().startsWith("EAT")) {
                if (animatedSprite.getSpritesSize() >= 28) {
                    return (direction.directionNumber - 5) * animatedSprite.getSpritesSize() / 4 + 3;
                } else {
                    return (direction.directionNumber - 5) * animatedSprite.getSpritesSize() / 4;
                }
            }
            if (direction.name().startsWith("SLEEP") || direction.name().startsWith("WAKEUP")) {
                return (direction.directionNumber - 9) * animatedSprite.getSpritesSize() / 4 + 7;
            }
            return direction.directionNumber * animatedSprite.getSpritesSize() / 4;
        }
        return 0;
    }

    protected int getEndSprite(Direction direction) {
        if (animatedSprite != null && direction != STAY) {
            if (direction.name().startsWith("EAT")) {
                if (animatedSprite.getSpritesSize() >= 28) {
                    return (direction.directionNumber - 5) * animatedSprite.getSpritesSize() / 4 + 6;
                } else {
                    return (direction.directionNumber - 5) * animatedSprite.getSpritesSize() / 4 + 2;
                }
            }
            if (direction.name().startsWith("SLEEP") || direction.name().startsWith("WAKEUP")) {
                return (direction.directionNumber - 9) * animatedSprite.getSpritesSize() / 4 + 10;
            }
            return direction.directionNumber * animatedSprite.getSpritesSize() / 4 + 2;
        }
        return 0;
    }

    @Override
    public void render(RenderHandler renderer, int zoom) {
        int xForSprite = animalRectangle.getX();
        int yForSprite = animalRectangle.getY();
        if (BABY.equals(age) && !animalType.contains("baby") || animalType.equals("chicken-baby")) {
            zoom = 1;
            xForSprite = animalRectangle.getX() + animalRectangle.getWidth() / 2;
            yForSprite = animalRectangle.getY() + animalRectangle.getHeight() / 2;
        }
        if (animatedSprite != null) {
            renderer.renderSprite(animatedSprite, xForSprite, yForSprite, zoom, false);
        }
        if (DEBUG_MODE) {
            Rectangle rectangle = new Rectangle(xForSprite, yForSprite, animalRectangle.getWidth(), animalRectangle.getHeight());
            if (interactionZone.isPlayerInRange()) {
                rectangle.generateBorder(2, GREEN);
            } else {
                rectangle.generateBorder(1, YELLOW);
            }
            renderer.renderRectangle(rectangle, zoom, false);
            interactionZone.render(renderer, zoom);
        }
        if (interactionZone.isPlayerInRange()) {
            heartIcon.render(renderer, 1);
        }
    }

    @Override
    public void update(Game game) {
        state.update(this, game);

        decreaseEnergyLevel();
        decreaseHungerLevel();
        decreaseThirstLevel();

        if (BABY.equals(age)) {
            updateAge();
        }
        interactionZone.update(game);
        if (interactionZone.isPlayerInRange()) {
            updateHeart(game);
        }
    }

    private void updateHeart(Game game) {
        int xPosition = animalRectangle.getX() - game.getRenderer().getCamera().getX() + 16;
        int yPosition = animalRectangle.getY() - game.getRenderer().getCamera().getY() - 16;
        Position heartPosition = new Position(xPosition, yPosition);
        heartIcon.changePosition(heartPosition);
    }

    protected void updateAge() {
        incrementAge();
        if (isTimeToGrowUp()) {
            setAge(ADULT);
            speed++;
        }
    }

    protected boolean isTimeToGrowUp() {
        return currentAge >= GROWING_UP_TIME;
    }

    protected void incrementAge() {
        currentAge++;
    }

    private void decreaseEnergyLevel() {
        if (!isSleeping() && currentEnergy > MIN_ENERGY) {
            currentEnergy--;
        }
        if (currentEnergy != 0 && currentEnergy % (MAX_ENERGY / 10) == 0) {
            logger.debug(String.format("Energy level for %s is %d percent", this, currentEnergy / (MAX_ENERGY / 100)));
        }
    }

    private boolean isSleeping() {
        return direction.name().startsWith("SLEEP");
    }

    public boolean isHungerLow() {
        return currentHunger < MAX_HUNGER / 100 * 70;
    }

    public boolean isThirstLow() {
        return currentThirst < MAX_THIRST / 100 * 70;
    }

    private void decreaseHungerLevel() {
        if (currentHunger > 0) {
            currentHunger--;
        }
        if (currentHunger != 0 && currentHunger % (MAX_HUNGER / 10) == 0) {
            logger.debug(String.format("Hunger level for %s is %d percent", this, currentHunger / (MAX_HUNGER / 100)));
        }
        if (currentHunger < MAX_HUNGER / 100 * 10) {
            speed = 1;
        }
    }

    private void decreaseThirstLevel() {
        if (currentThirst > 0) {
            currentThirst--;
        }
        if (currentThirst != 0 && currentThirst % (MAX_THIRST / 10) == 0) {
            logger.debug(String.format("Thirst level for %s is %d percent", this, currentThirst / (MAX_THIRST / 100)));
        }
        if (currentThirst < MAX_THIRST / 100 * 10) {
            speed = 1;
        }
    }

    protected void resetSpeedToDefault() {
        if (BABY.equals(age)) {
            setSpeed(2);
        } else {
            setSpeed(3);
        }
    }

    public boolean isAnimalStuck(GameMap gameMap) {
        return unwalkableInThisDirection(gameMap, LEFT, animalRectangle, speed, getLayer())
                && unwalkableInThisDirection(gameMap, RIGHT, animalRectangle, speed, getLayer())
                && unwalkableInThisDirection(gameMap, UP, animalRectangle, speed, getLayer())
                && unwalkableInThisDirection(gameMap, DOWN, animalRectangle, speed, getLayer());
    }

    public void tryToMove(GameMap gameMap) {
        logger.info(String.format("Animal %s is stuck, will try to move to nearest directions", this));
        route = new Route();
        for (Direction potentialDirection : Direction.values()) {
            if (!unwalkableInThisDirection(gameMap, potentialDirection, animalRectangle, speed, getLayer())) {
                moveAnimalTo(potentialDirection);
            }
        }
        if (isAnimalStuck(gameMap)) {
            logger.info("Animal still stuck, will try to move to center");
            moveAnimalToCenter(gameMap);
        }
        if (isAnimalStuck(gameMap)) {
            logger.error("Animal is stuck completely");
        }
    }

    public void moveAnimalTo(Direction direction) {
        switch (direction) {
            case LEFT:
                animalRectangle.setX(animalRectangle.getX() - (TILE_SIZE * ZOOM));
                break;
            case RIGHT:
                animalRectangle.setX(animalRectangle.getX() + (TILE_SIZE * ZOOM));
                break;
            case UP:
                animalRectangle.setY(animalRectangle.getY() - (TILE_SIZE * ZOOM));
                break;
            case DOWN:
                animalRectangle.setY(animalRectangle.getY() + (TILE_SIZE * ZOOM));
                break;
        }
    }

    @Override
    public int getLayer() {
        return 2;
    }

    @Override
    public boolean handleMouseClick(Rectangle mouseRectangle, Rectangle camera, int zoom, Game game) {
        if (mouseRectangle.intersects(animalRectangle)) {
            logger.info("Click on Animal: ");
            moveAnimalToCenter(game.getGameMap());
            return true;
        }
        return false;
    }

    private void moveAnimalToCenter(GameMap gameMap) {
        animalRectangle.setX(gameMap.getMapWidth() * TILE_SIZE * ZOOM / 2);
        animalRectangle.setY(gameMap.getMapHeight() * TILE_SIZE * ZOOM / 2);

        if (isAnimalStuck(gameMap)) {
            tryToMove(gameMap);
        }
    }

    public void teleportAnimalTo(int x, int y) {
        animalRectangle.setX(x);
        animalRectangle.setY(y);
    }

    public void sendToNpc(Route route) {
        setWalkingState();
        walkingState.sendToNpc(this, route);
    }

    public void goAway(Route route) {
        setWalkingState();
        walkingState.goAway(this, route);
    }

    // TODO: should be different for each language
    @Override
    public String toString() {
        return animalType + " named " + name;
    }

    /**
     * =================================== STATES ======================================
     */

    public void setWalkingState() {
        state = walkingState;
    }

    public void setWaitingState() {
        state = waitingState;
    }

    public void setEatingState() {
        state = eatingState;
    }

    public void setSleepingState() {
        state = sleepingState;
    }

    public void setWakingUpState() {
        state = wakingUpState;
    }

    public void setFallingAsleepState() {
        state = fallingAsleepState;
    }

    /**
     * =================================== GETTERS & SETTERS ======================================
     */

    public int getSpeed() {
        return speed;
    }

    public int getCurrentX() {
        return animalRectangle.getX();
    }

    public int getCurrentY() {
        return animalRectangle.getY();
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getCurrentHunger() {
        return currentHunger;
    }

    public int getCurrentThirst() {
        return currentThirst;
    }

    public int getCurrentEnergy() {
        return currentEnergy;
    }

    public void setCurrentHunger(int currentHunger) {
        this.currentHunger = currentHunger;
    }

    public void setCurrentThirst(int currentThirst) {
        this.currentThirst = currentThirst;
    }

    public void setCurrentEnergy(int currentEnergy) {
        this.currentEnergy = currentEnergy;
    }

    public void setEnergyInPercent(int energy) {
        logger.debug(String.format("Setting current energy to %d percent", energy));
        this.currentEnergy = MAX_ENERGY / 100 * energy;
    }

    public void setThirstInPercent(int thirst) {
        logger.debug(String.format("Setting current thirst to %d percent", thirst));
        this.currentThirst = MAX_THIRST / 100 * thirst;
    }

    public void setHungerInPercent(int hunger) {
        logger.debug(String.format("Setting current hunger to %d percent", hunger));
        this.currentHunger = MAX_HUNGER / 100 * hunger;
    }

    public int getCurrentHungerInPercent() {
        return currentHunger / (MAX_HUNGER / 100);
    }

    public int getCurrentThirstInPercent() {
        return currentThirst / (MAX_THIRST / 100);
    }

    public int getCurrentEnergyInPercent() {
        return currentEnergy / (MAX_ENERGY / 100);
    }

    public String getAnimalType() {
        return animalType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public AgeStage getAge() {
        return age;
    }

    public void setAge(AgeStage age) {
        this.age = age;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setAnimalType(String animalType) {
        this.animalType = animalType;
    }

    public int getCurrentAge() {
        return currentAge;
    }

    public void setCurrentAge(int currentAge) {
        this.currentAge = currentAge;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Rectangle getRectangle() {
        return animalRectangle;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getCurrentMap() {
        return currentMap;
    }

    public void setCurrentMap(String currentMap) {
        this.currentMap = currentMap;
    }

    public Sprite getPreviewSprite() {
        if (previewSprite != null) {
            return previewSprite;
        }
        return animatedSprite.getStartSprite();
    }

    public AnimatedSprite getAnimatedSprite() {
        return animatedSprite;
    }

    public InteractionZonePetHeart getInteractionZone() {
        return interactionZone;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }
}
