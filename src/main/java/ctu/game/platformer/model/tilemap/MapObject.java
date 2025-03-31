package ctu.game.platformer.model.tilemap;

import ctu.game.platformer.model.common.GameObject;

public class MapObject extends GameObject {
    private String type;

    public MapObject(float x, float y, float width, float height, String type) {
        super(x, y, width, height);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public void update() {
        // Object-specific update logic can be added here
    }
}