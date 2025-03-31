package ctu.game.platformer.model.level;

import com.fasterxml.jackson.databind.ObjectMapper;
import ctu.game.platformer.model.tilemap.TileMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class LevelManager {
    @Autowired
    private TileMap tileMap;

    private Map<String, LevelData> levels = new HashMap<>();
    private String currentLevelId;

    public void initialize() {
        try (InputStream inputStream = getClass().getResourceAsStream("/maps/level.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: /maps/level.json");
            }

            ObjectMapper mapper = new ObjectMapper();
            LevelConfig config = mapper.readValue(inputStream, LevelConfig.class);

            if (config.getLevels() == null || config.getLevels().isEmpty()) {
                throw new IOException("No levels found in JSON!");
            }

            for (LevelData level : config.getLevels()) {
                levels.put(level.getId(), level);
            }

            // Set initial level
            loadLevel(config.getLevels().get(0).getId());
        } catch (IOException e) {
            System.err.println("Error loading level data: " + e.getMessage());
        }
    }


    public void loadLevel(String levelId) {
        if (!levels.containsKey(levelId)) {
            System.err.println("Level not found: " + levelId);
            return;
        }

        LevelData level = levels.get(levelId);
        tileMap.loadMap(level.getMapFile());
        currentLevelId = levelId;
    }

    public void transitionTo(String transitionId) {
        LevelData currentLevel = levels.get(currentLevelId);
        TransitionPoint transition = currentLevel.getTransitions().get(transitionId);

        if (transition == null) {
            System.err.println("Transition point not found: " + transitionId);
            return;
        }

        String nextLevelId = transition.getTargetLevel();
        if (!levels.containsKey(nextLevelId)) {
            System.err.println("Target level not found: " + nextLevelId);
            return;
        }

        loadLevel(nextLevelId);
    }

    public float[] getPlayerStartPosition() {
        LevelData level = levels.get(currentLevelId);
        return new float[]{level.getStartX(), level.getStartY()};
    }

    public String getCurrentLevelId() {
        return currentLevelId;
    }

    public LevelData getLevelData(String levelId) {
        return levels.get(levelId);
    }
}