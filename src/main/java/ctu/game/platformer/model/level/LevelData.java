package ctu.game.platformer.model.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelData {
    private String id;
    private String mapFile;
    private float startX;
    private float startY;
    private Map<String, TransitionPoint> transitions = new HashMap<>();

    private String backgroundFilename;

    public String getBackgroundFilename() {
        return backgroundFilename;
    }

    public void setBackgroundFilename(String backgroundFilename) {
        this.backgroundFilename = backgroundFilename;
    }

    public LevelData(String id, String mapFile, float startX, float startY) {
        this.id = id;
        this.mapFile = mapFile;
        this.startX = startX;
        this.startY = startY;
    }


    public void addTransition(String id, TransitionPoint point) {
        transitions.put(id, point);
    }

    public String getId() {
        return id;
    }

    public String getMapFile() {
        return mapFile;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public Map<String, TransitionPoint> getTransitions() {
        return transitions;
    }
}