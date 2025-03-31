package ctu.game.platformer.model.platformer;

import ctu.game.platformer.model.common.GameObject;
import ctu.game.platformer.model.tilemap.TileMap;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Player extends GameObject {
    private float velocityX = 0;
    private float velocityY = 0;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean jumping = false;
    private boolean isOnGround = false;
    private final float speed = 5.0f;
    private final float jumpForce = -15.0f;
    private final float gravity = 0.8f;
    private final float maxFallSpeed = 20.0f;
    private TileMap tileMap;
    private boolean showCollision = true;
    // In Player.java, add these fields:
    private float jumpVelocityX = -8.0f; // Horizontal velocity when jumping left
    private float jumpVelocityRight = 8.0f; // Horizontal velocity when jumping right
    private final float airControlFactor = 0.3f;
    // Smaller step for more precise collision detection
    private final float COLLISION_STEP = 1.0f;



    private int textureId = -1;
    private int spriteRows = 3;
    private int spriteColumns = 8;
    private int currentFrame = 0;
    private int currentRow = 0; // 0=idle, 1=run, 2=jump/fall

    private long lastFrameTime = 0;
    private long frameDuration = 100; // milliseconds between frame changes
    private int animationRow = 0;

    private boolean facingRight = true;
    private boolean isloadSprite = false;

    // Update the PlayerState enum to include more specific states
    public enum PlayerState {
        IDLE, RUN_RIGHT, RUN_LEFT, JUMP_UP, JUMP_RIGHT, JUMP_LEFT, FALLING
    }

    // Add these fields to track the current state
    private PlayerState currentState = PlayerState.IDLE;
    private PlayerState previousState = PlayerState.IDLE;

    public void loadSprite(String spritePath) {
        try {
            // Load the image from classpath resources
            InputStream is = getClass().getClassLoader().getResourceAsStream(spritePath);
            if (is == null) {
                throw new IOException("Cannot find resource: " + spritePath);
            }

            BufferedImage image = ImageIO.read(is);
            int width = image.getWidth();
            int height = image.getHeight();

            // Convert the image to a byte buffer
            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = image.getRGB(x, y);
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            // Generate texture
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            // Set texture parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            // Upload texture data
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
            isloadSprite=true;
            System.out.println("Sprite loaded successfully: " + width + "x" + height);
        } catch (IOException e) {
            System.err.println("Failed to load sprite: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    private void updateAnimation() {
        // Determine current state based on movement and direction
        previousState = currentState;

        if (!isOnGround) {
            if (velocityY < 0) { // Going up (jumping)
                if (velocityX < -0.1f) {
                    currentState = PlayerState.JUMP_LEFT;
                } else if (velocityX > 0.1f) {
                    currentState = PlayerState.JUMP_RIGHT;
                } else {
                    currentState = PlayerState.JUMP_UP;
                }
            } else { // Going down (falling)
                currentState = PlayerState.FALLING;
            }
        } else if (Math.abs(velocityX) > 0.1f) {
            currentState = velocityX > 0 ? PlayerState.RUN_RIGHT : PlayerState.RUN_LEFT;
        } else {
            currentState = PlayerState.IDLE;
        }

        // Set column based on the specific state and animate through rows
        switch (currentState) {
            case IDLE:
                currentFrame = 4; // Col 5
                break;
            case RUN_RIGHT:
                currentFrame = 2; // Col 3
                facingRight = true;
                break;
            case RUN_LEFT:
                currentFrame = 6; // Col 7
                facingRight = false;
                break;
            case JUMP_UP:
                currentFrame = 0; // Col 1
                break;
            case JUMP_RIGHT:
                currentFrame = 3; // Col 4
                facingRight = true;
                break;
            case JUMP_LEFT:
                currentFrame = 7; // Col 8
                facingRight = false;
                break;
            case FALLING:
                currentFrame = 4; // Same as idle
                break;
        }

        // Animate through rows (3 frames per animation)
        long now = System.currentTimeMillis();
        if (now - lastFrameTime > frameDuration) {
            animationRow = (animationRow + 1) % 3;
            lastFrameTime = now;
        }

        // Set the current row to the animation frame
        currentRow = animationRow;
    }

    @Override
    public void update() {
        // Ground movement has full control
        if (isOnGround) {
            velocityX = 0;
            if (movingLeft) velocityX -= speed;
            if (movingRight) velocityX += speed;
            jumping = false; // Reset jumping state when on ground
        } else {
            // In mid-air: player has some control but momentum persists
            if (movingLeft) {
                velocityX = Math.max(velocityX - speed * airControlFactor, -speed);
            }
            if (movingRight) {
                velocityX = Math.min(velocityX + speed * airControlFactor, speed);
            }

            // Apply air resistance (less than before for better momentum feel)
            velocityX *= 0.98f;
        }

        // Apply gravity
        velocityY += gravity;

        // Cap falling speed
        if (velocityY > maxFallSpeed) {
            velocityY = maxFallSpeed;
        }

        // Apply movement with improved collision detection
        moveWithCollisionCheck();

        // Check for ground after movement
        checkGroundContact();
    }
    private void checkGroundContact() {
        // Check if there's ground 1 pixel below the player
        isOnGround = checkCollision(getX(), getY() + getHeight() + 1) ||
                checkCollision(getX() + getWidth()/2, getY() + getHeight() + 1) ||
                checkCollision(getX() + getWidth(), getY() + getHeight() + 1);
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

                if (!checkCollision(getX(), newY)) {
                    setPosition(getX(), newY);
                } else {
                    // Hit something - if moving down, we hit ground
                    if (velocityY > 0) {
                        isOnGround = true;
                    }
                    // Reset vertical velocity on collision
                    velocityY = 0;
                    break;
                }

                remainingY -= stepY;
            }
        }
    }


    public void render() {
        if(isloadSprite==false){
            loadSprite("assets/images/platformer/player.png");
        }

        if (textureId != -1) {
            updateAnimation();

            // Enable texturing
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, textureId);

            // Calculate texture coordinates for a 3×8 grid
            float frameWidth = 1.0f / spriteColumns;  // 1/8 = 0.125
            float frameHeight = 1.0f / spriteRows;    // 1/3 = 0.333

            float s1 = currentFrame * frameWidth;
            float s2 = s1 + frameWidth;
            float t1 = currentRow * frameHeight;
            float t2 = t1 + frameHeight;

//            // Flip texture coordinates horizontally if facing left
//            if (!facingRight) {
//                float temp = s1;
//                s1 = s2;
//                s2 = temp;
//            }

            // Draw textured quad matching your 16×23px ratio
            float spriteRatio = 16.0f / 23.0f;
            float renderHeight = getHeight();
            float renderWidth = renderHeight * spriteRatio;

            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            glBegin(GL_QUADS);
            glTexCoord2f(s1, t1); glVertex2f(getX(), getY());
            glTexCoord2f(s2, t1); glVertex2f(getX() + renderWidth, getY());
            glTexCoord2f(s2, t2); glVertex2f(getX() + renderWidth, getY() + renderHeight);
            glTexCoord2f(s1, t2); glVertex2f(getX(), getY() + renderHeight);
            glEnd();

            glDisable(GL_TEXTURE_2D);
        } else {
            // Fallback rendering code
            glColor3f(1.0f, 0.0f, 0.0f);
            glBegin(GL_QUADS);
            glVertex2f(getX(), getY());
            glVertex2f(getX() + getWidth(), getY());
            glVertex2f(getX() + getWidth(), getY() + getHeight());
            glVertex2f(getX(), getY() + getHeight());
            glEnd();
        }

        // Draw collision box if enabled
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
        float inset = 2.0f;  // Increased inset for better corner handling

        // Check multiple points around the player's hitbox for more reliable collision
        return
                // Top edge (3 points)
                tileMap.isSolid(x + inset, y) ||
                        tileMap.isSolid(x + getWidth()/2, y) ||
                        tileMap.isSolid(x + getWidth() - inset, y) ||

                        // Middle (2 points on sides)
                        tileMap.isSolid(x, y + getHeight()/4) ||
                        tileMap.isSolid(x, y + getHeight()/2) ||
                        tileMap.isSolid(x, y + getHeight()*3/4) ||
                        tileMap.isSolid(x + getWidth(), y + getHeight()/4) ||
                        tileMap.isSolid(x + getWidth(), y + getHeight()/2) ||
                        tileMap.isSolid(x + getWidth(), y + getHeight()*3/4) ||

                        // Bottom edge (3 points)
                        tileMap.isSolid(x + inset, y + getHeight()) ||
                        tileMap.isSolid(x + getWidth()/2, y + getHeight()) ||
                        tileMap.isSolid(x + getWidth() - inset, y + getHeight());
    }

    // Toggle collision visualization
    public void toggleCollisionView() {
        showCollision = !showCollision;
    }

    public void cleanup() {
        if (textureId != -1) {
            glDeleteTextures(textureId);
            textureId = -1;
        }
    }
    // Jump method
    public void jump() {
        if (isOnGround) {
            velocityY = jumpForce;
            isOnGround = false;

            // Apply horizontal boost based on direction
            if (movingLeft) {
                velocityX = jumpVelocityX * 1.5f;
            } else if (movingRight) {
                velocityX = jumpVelocityRight * 1.5f;
            }

            // Play jump sound
            // audioManager.playSoundEffect("jump.wav");
        }
    }

    // Getters and setters for movement flags
    public void setMovingLeft(boolean movingLeft) {
        this.movingLeft = movingLeft;
    }

    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }

    // Removed up/down movement methods as they're not needed in a platformer

    // New getters for platformer states
    public boolean isOnGround() {
        return isOnGround;
    }
}