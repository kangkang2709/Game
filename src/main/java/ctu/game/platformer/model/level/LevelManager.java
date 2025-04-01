package ctu.game.platformer.model.level;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ctu.game.platformer.model.tilemap.TileMap;
import ctu.game.platformer.util.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LevelManager {
    @Autowired
    private TileMap tileMap;

    @Autowired
    private ResourceLoader resourceLoader;

    private Map<String, LevelData> levels = new HashMap<>();
    private String currentLevelId = "level1"; // Default starting level

    public void initialize() {
        loadLevelConfig();
        loadLevel(currentLevelId);
    }

    private void loadLevelConfig() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("maps/level.json");
            if (is == null) {
                throw new RuntimeException("Cannot find level config file");
            }

            Gson gson = new Gson();
            JsonObject config = gson.fromJson(new InputStreamReader(is), JsonObject.class);
            JsonArray levelsArray = config.getAsJsonArray("levels");

            for (JsonElement levelElement : levelsArray) {
                JsonObject levelObj = levelElement.getAsJsonObject();

                String id = levelObj.get("id").getAsString();
                String mapFile = levelObj.get("mapFile").getAsString();
                float startX = levelObj.get("startX").getAsFloat();
                float startY = levelObj.get("startY").getAsFloat();

                LevelData levelData = new LevelData(id, mapFile, startX, startY);

                // Load backgrounds
                // Inside loadLevelConfig() method
                if (levelObj.has("backgrounds")) {
                    if (levelObj.get("backgrounds").isJsonPrimitive()) {
                        levelData.setBackgroundFilename(levelObj.get("backgrounds").getAsString());
                    }
                }

                // Load transitions
                if (levelObj.has("transitions")) {
                    JsonObject transitions = levelObj.getAsJsonObject("transitions");
                    for (Map.Entry<String, JsonElement> entry : transitions.entrySet()) {
                        JsonObject tp = entry.getValue().getAsJsonObject();
                        float x = tp.get("x").getAsFloat();
                        float y = tp.get("y").getAsFloat();
                        String targetLevel = tp.get("targetLevel").getAsString();

                        levelData.addTransition(entry.getKey(), new TransitionPoint(x, y, targetLevel));
                    }
                }

                levels.put(id, levelData);
            }
        } catch (Exception e) {
            System.err.println("Error loading level config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean loadLevel(String levelId) {
        if (!levels.containsKey(levelId)) {
            System.err.println("Level not found: " + levelId);
            return false;
        }

        LevelData levelData = levels.get(levelId);
        currentLevelId = levelId;

        try {
            // Load the tile map
            tileMap.switchToLayer(0);
            tileMap.loadMap(levelData.getMapFile());
            tileMap.setBackground(levelData.getBackgroundFilename());
            return true;
        } catch (Exception e) {
            System.err.println("Error loading level: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public float[] getPlayerStartPosition() {
        LevelData levelData = levels.get(currentLevelId);
        return new float[] { levelData.getStartX(), levelData.getStartY() };
    }

    public String getCurrentLevelId() {
        return currentLevelId;
    }

    public LevelData getLevelData(String levelId) {
        return levels.get(levelId);
    }
}