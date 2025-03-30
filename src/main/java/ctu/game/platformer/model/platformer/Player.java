package ctu.game.platformer.model.platformer;

import ctu.game.platformer.model.common.GameObject;

public class Player extends GameObject {
    private float speed = 5.0f; // Direct movement speed
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;

    public Player(float x, float y) {
        super(x, y, 50, 50);
    }

    @Override
    public void update() {
        float dx = 0;
        float dy = 0;

        // Only move when keys are actually pressed
        if (movingLeft) dx -= speed;
        if (movingRight) dx += speed;
        if (movingUp) dy -= speed;
        if (movingDown) dy += speed;

        // Update position only if movement is needed
        if (dx != 0 || dy != 0) {
            setPosition(getX() + dx, getY() + dy);
        }
    }

    // Set movement state methods
    public void setMovingLeft(boolean moving) {
        this.movingLeft = moving;
    }

    public void setMovingRight(boolean moving) {
        this.movingRight = moving;
    }

    public void setMovingUp(boolean moving) {
        this.movingUp = moving;
    }

    public void setMovingDown(boolean moving) {
        this.movingDown = moving;
    }

    // Reset all movement
    public void stopAllMovement() {
        movingLeft = false;
        movingRight = false;
        movingUp = false;
        movingDown = false;
    }
}