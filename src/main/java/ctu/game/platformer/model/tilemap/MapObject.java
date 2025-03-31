package ctu.game.platformer.model.tilemap;

import ctu.game.platformer.model.common.GameObject;

public class MapObject {
    private float x, y;
    private int width, height;
    private String type;
    private String mapFilename; // Add this field

    public MapObject(float x, float y, int width, int height, String type, String mapFilename) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.mapFilename = mapFilename; // Initialize this field
    }

    // Getters and setters for the new field
    public String getMapFilename() {
        return mapFilename;
    }

    public void setMapFilename(String mapFilename) {
        this.mapFilename = mapFilename;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
// Other getters and setters...
}