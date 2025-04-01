package ctu.game.platformer.model.level;

import java.util.HashMap;
import java.util.Map;

public class LevelData {
    private String id;
    private String mapFile;
    private float startX;
    private float startY;
    private Map<String, TransitionPoint> transitions = new HashMap<>();

    // Default constructor for JSON parsing
    public LevelData() {
    }

    public LevelData(String id, String mapFile, float startX, float startY) {
        this.id = id;
        this.mapFile = mapFile;
        this.startX = startX;
        this.startY = startY;
    }

    public void addTransition(String id, float x, float y, String targetLevel) {
        transitions.put(id, new TransitionPoint(x, y, targetLevel));
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMapFile() {
        return mapFile;
    }

    public void setMapFile(String mapFile) {
        this.mapFile = mapFile;
    }

    public float getStartX() {
        return startX;
    }

    public void setStartX(float startX) {
        this.startX = startX;
    }

    public float getStartY() {
        return startY;
    }

    public void setStartY(float startY) {
        this.startY = startY;
    }

    public Map<String, TransitionPoint> getTransitions() {
        return transitions;
    }

    public void setTransitions(Map<String, TransitionPoint> transitions) {
        this.transitions = transitions;
    }
}
