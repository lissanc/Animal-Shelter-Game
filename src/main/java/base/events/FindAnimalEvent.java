package base.events;

import base.Game;
import base.gameobjects.Animal;
import base.gameobjects.AnimalService;
import base.gameobjects.Chest;
import base.gameobjects.animals.Cat;
import base.map.GameMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static base.constants.Constants.*;

public class FindAnimalEvent extends Event {

    private static final Logger logger = LoggerFactory.getLogger(FindAnimalEvent.class);

    public FindAnimalEvent() {
        repeatable = true;
    }

    @Override
    void calculateChance(Game game) {
        chance = 5;
        if (!BOTTOM_MAPS.contains(game.getGameMap().getMapName()) || (!repeatable && happened)) {
            chance = 0;
            return;
        }
        if (happened) {
            chance--;
        }
        if (BOTTOM_LEFT_MAP.equals(game.getGameMap().getMapName()) || BOTTOM_CENTER_MAP.equals(game.getGameMap().getMapName())) {
            chance++;
        }
        if (game.isBackpackEmpty()) {
            chance--;
        } else {
            chance++;
        }
        for (List<Animal> animalList : game.getAnimalsOnMaps().values()) {
            chance -= animalList.size();
        }
        logger.info(String.format("Event 'Find Random Animal' chance is %d", chance));
    }

    @Override
    void startEvent(Game game) {
        GameMap map = game.getGameMap();
        int x = random.nextInt(map.getMapWidth() - 1);
        int y = random.nextInt(map.getMapHeight() - 2);

        logger.info(String.format("Random chest will appear at %d and %d", x, y));

        int bigX = x * (TILE_SIZE * ZOOM);
        int bigY = y * (TILE_SIZE * ZOOM);
        if (map.isPlaceEmpty(2, bigX, bigY)) {
            logger.info("Place was empty, will put chest");
            AnimalService animalService = game.getAnimalService();
            int animalId = random.nextInt(animalService.animalNames.size() - 1);
            String animalType = animalService.animalNames.get(animalId);
            if (animalId == animalService.animalNames.indexOf(Cat.NAME)) {
                int catType = random.nextInt(CAT_COLORS.size() - 1);
                animalType = CAT_COLORS.get(catType);
            }
            logger.info(String.format("Random animal is %s", animalType));
            Animal animal = animalService.createAnimal(bigX, bigY + 64, animalType, game.getGameMap().getMapName());
            map.addObject(new Chest(bigX, bigY, animal, game.getTileService().getTiles().get(36).getSprite()));
            game.saveMap();
        }
        happened = true;
        logger.info("Event for new animal started");
    }
}
