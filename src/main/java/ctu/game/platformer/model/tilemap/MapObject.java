package ctu.game.platformer.model.tilemap;

import ctu.game.platformer.model.common.GameObject;

public class MapObject {
    private float x, y;
    private float width, height;
    private String type;
    private String mapFilename;
    private int targetLayer = -1; // For layer portal objects

    public MapObject(float x, float y, float width, float height, String type, String mapFilename) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.mapFilename = mapFilename;
    }

    // Add constructor with layer information
    public MapObject(float x, float y, float width, float height, String type, String mapFilename, int targetLayer) {
        this(x, y, width, height, type, mapFilename);
        this.targetLayer = targetLayer;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMapFilename(String mapFilename) {
        this.mapFilename = mapFilename;
    }

    // Getters and setters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public String getType() { return type; }
    public String getMapFilename() { return mapFilename; }
    public int getTargetLayer() { return targetLayer; }
    public void setTargetLayer(int targetLayer) { this.targetLayer = targetLayer; }
}