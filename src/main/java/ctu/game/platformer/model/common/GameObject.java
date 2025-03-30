// src/main/java/ctu/game/flatformer/model/common/GameObject.java
package ctu.game.platformer.model.common;

public abstract class GameObject {
    private Position position;
    private float width;
    private float height;

    public GameObject(float x, float y, float width, float height) {
        this.position = new Position(x, y);
        this.width = width;
        this.height = height;
    }

    public Position getPosition() {
        return position;
    }

    public float getX() {
        return position.getX();
    }

    public float getY() {
        return position.getY();
    }

    public void setPosition(float x, float y) {
        position.setX(x);
        position.setY(y);
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public abstract void update();
}