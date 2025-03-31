package ctu.game.platformer.model.platformer;

import ctu.game.platformer.model.common.GameObject;
import ctu.game.platformer.model.tilemap.TileMap;
import org.lwjgl.opengl.GL11;

public class Player extends GameObject {
    private float velocityX = 0;
    private float velocityY = 0;
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private final float speed = 5.0f;
    private TileMap tileMap;
    private boolean showCollision = true;

    // Smaller step for more precise collision detection
    private final float COLLISION_STEP = 1.0f;

    public Player(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    public Player(float x, float y) {
        super(x, y, 32, 64); // Default size
    }

    public void setTileMap(TileMap tileMap) {
        this.tileMap = tileMap;
        if (tileMap != null) {
            this.showCollision = tileMap.isShowingCollision();
        }
    }

    @Override
    public void update() {
        // Calculate desired velocity based on input
        velocityX = 0;
        velocityY = 0;

        if (movingUp) velocityY -= speed;
        if (movingDown) velocityY += speed;
        if (movingLeft) velocityX -= speed;
        if (movingRight) velocityX += speed;

        // Apply movement with improved collision detection
        moveWithCollisionCheck();
    }

    private void moveWithCollisionCheck() {
        if (tileMap == null) {
            setPosition(getX() + velocityX, getY() + velocityY);
            return;
        }

        // Move along X axis with proper collision detection
        float remainingX = velocityX;
        if (remainingX != 0) {
            float directionX = Math.signum(remainingX);
            while (Math.abs(remainingX) > 0) {
                float stepX = directionX * Math.min(COLLISION_STEP, Math.abs(remainingX));
                float newX = getX() + stepX;

                // Use the comprehensive checkCollision method
                if (!checkCollision(newX, getY())) {
                    setPosition(newX, getY());
                } else {
                    // Stop X movement if collision detected
                    break;
                }

                remainingX -= stepX;
            }
        }

        // Move along Y axis with proper collision detection
        float remainingY = velocityY;
        if (remainingY != 0) {
            float directionY = Math.signum(remainingY);
            while (Math.abs(remainingY) > 0) {
                float stepY = directionY * Math.min(COLLISION_STEP, Math.abs(remainingY));
                float newY = getY() + stepY;

                // Use the comprehensive checkCollision method
                if (!checkCollision(getX(), newY)) {
                    setPosition(getX(), newY);
                } else {
                    // Stop Y movement if collision detected
                    break;
                }

                remainingY -= stepY;
            }
        }
    }

    public void render() {
        // Draw player
        GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(getX(), getY());
        GL11.glVertex2f(getX() + getWidth(), getY());
        GL11.glVertex2f(getX() + getWidth(), getY() + getHeight());
        GL11.glVertex2f(getX(), getY() + getHeight());
        GL11.glEnd();

        // Draw collision box
        if (showCollision) {
            renderCollisionBox();
        }
    }

    private void renderCollisionBox() {
        // Draw the outline
        GL11.glColor4f(0.0f, 1.0f, 0.0f, 0.7f); // Green with transparency
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(getX(), getY());
        GL11.glVertex2f(getX() + getWidth(), getY());
        GL11.glVertex2f(getX() + getWidth(), getY() + getHeight());
        GL11.glVertex2f(getX(), getY() + getHeight());
        GL11.glEnd();

        // Draw the collision check points
        float inset = 0.1f;

        // Top edge (3 points)
        drawCollisionPoint(getX() + inset, getY(), 3.0f);
        drawCollisionPoint(getX() + getWidth()/2, getY(), 3.0f);
        drawCollisionPoint(getX() + getWidth() - inset, getY(), 3.0f);

        // Middle (2 points on sides)
        drawCollisionPoint(getX(), getY() + getHeight()/2, 3.0f);
        drawCollisionPoint(getX() + getWidth(), getY() + getHeight()/2, 3.0f);

        // Bottom edge (3 points)
        drawCollisionPoint(getX() + inset, getY() + getHeight(), 3.0f);
        drawCollisionPoint(getX() + getWidth()/2, getY() + getHeight(), 3.0f);
        drawCollisionPoint(getX() + getWidth() - inset, getY() + getHeight(), 3.0f);
    }

    private void drawCollisionPoint(float x, float y, float size) {
        GL11.glColor3f(0.0f, 0.5f, 1.0f); // Blue
        GL11.glPointSize(size);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
    }

    private boolean checkCollision(float x, float y) {
        if (tileMap == null) return false;

        // Create a small inset to avoid edge cases
        float inset = 0.1f;

        // Check 8 points around the player's hitbox for more reliable collision
        return
                // Top edge (3 points)
                tileMap.isSolid(x + inset, y) ||
                        tileMap.isSolid(x + getWidth()/2, y) ||
                        tileMap.isSolid(x + getWidth() - inset, y) ||

                        // Middle (2 points on sides)
                        tileMap.isSolid(x, y + getHeight()/2) ||
                        tileMap.isSolid(x + getWidth(), y + getHeight()/2) ||

                        // Bottom edge (3 points)
                        tileMap.isSolid(x + inset, y + getHeight()) ||
                        tileMap.isSolid(x + getWidth()/2, y + getHeight()) ||
                        tileMap.isSolid(x + getWidth() - inset, y + getHeight());
    }

    // Toggle collision visualization
    public void toggleCollisionView() {
        showCollision = !showCollision;
    }

    // Getters and setters for movement flags
    public void setMovingUp(boolean movingUp) {
        this.movingUp = movingUp;
    }

    public void setMovingDown(boolean movingDown) {
        this.movingDown = movingDown;
    }

    public void setMovingLeft(boolean movingLeft) {
        this.movingLeft = movingLeft;
    }

    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }
}