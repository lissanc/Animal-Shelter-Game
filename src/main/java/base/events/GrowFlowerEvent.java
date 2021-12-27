package base.events;

import base.Game;
import base.map.GameMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static base.constants.Constants.*;
import static base.constants.MultiOptionalObjects.flowerIds;

public class GrowFlowerEvent extends Event{

    protected static final Logger logger = LoggerFactory.getLogger(GrowFlowerEvent.class);

    public GrowFlowerEvent() {
        repeatable = true;
    }

    @Override
    void calculateChance(Game game) {
        chance = 1;
        if (!repeatable && happened) {
            chance = 0;
            return;
        }
        if (happened) {
            chance--;
        }
        if (OUTSIDE_MAPS.contains(game.getGameMap().getMapName())) {
            chance++;
        }
        if (HOME_MAPS.contains(game.getGameMap().getMapName())) {
            chance--;
        }
        logger.info(String.format("Event 'Grow Random Flower' chance is %d", chance));
    }

    @Override
    void startEvent(Game game) {
        GameMap map = game.getGameMap();
        int x = random.nextInt(map.getMapWidth());
        int y = random.nextInt(map.getMapHeight());
        int bigX = x  * (TILE_SIZE * ZOOM);
        int bigY = y  * (TILE_SIZE * ZOOM);
        logger.info(String.format("Random flower will appear at %d and %d", x, y));
        if (map.isThereGrassOrDirt(bigX, bigY) && map.isPlaceEmpty(1, bigX, bigY)) {
            int flowerId = random.nextInt(flowerIds.size());
            logger.info(String.format("Place was empty, will add flower with id %d", flowerId));
            map.setTile(x, y, flowerIds.get(flowerId), true);
        }
    }

}
