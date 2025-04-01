package ctu.game.platformer.model.tilemap;
import ctu.game.platformer.model.tilemap.TextureManager;
import org.lwjgl.opengl.GL11;

public class Background {
    private int textureId;
    private float scrollSpeedX;
    private float scrollSpeedY;
    private float offsetX = 0;
    private float offsetY = 0;
    private boolean tiled = true;
    private int width;
    private int height;

    public Background(String texturePath, float scrollSpeedX, float scrollSpeedY, boolean tiled) {
        this.textureId = TextureManager.loadTexture(texturePath, true);
        this.scrollSpeedX = scrollSpeedX;
        this.scrollSpeedY = scrollSpeedY;
        this.tiled = tiled;

        // Get texture dimensions
        this.width = 1;
        this.height = 1;
        if (textureId != -1) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            this.width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            this.height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        }
    }

    public void update(float deltaTime, float cameraX, float cameraY) {
        // Update parallax scrolling offsets
        offsetX = (cameraX * scrollSpeedX) % width;
        offsetY = (cameraY * scrollSpeedY) % height;
    }

    public void render(float cameraX, float cameraY, int screenWidth, int screenHeight) {
        if (textureId == -1) return;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Calculate screen coordinates
        float screenLeft = cameraX - screenWidth/2;
        float screenTop = cameraY - screenHeight/2;

        if (tiled) {
            // Draw tiled background with parallax effect
            float startX = -offsetX;
            while (startX < screenWidth) {
                float startY = -offsetY;
                while (startY < screenHeight) {
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glTexCoord2f(0, 0); GL11.glVertex2f(screenLeft + startX, screenTop + startY);
                    GL11.glTexCoord2f(1, 0); GL11.glVertex2f(screenLeft + startX + width, screenTop + startY);
                    GL11.glTexCoord2f(1, 1); GL11.glVertex2f(screenLeft + startX + width, screenTop + startY + height);
                    GL11.glTexCoord2f(0, 1); GL11.glVertex2f(screenLeft + startX, screenTop + startY + height);
                    GL11.glEnd();
                    startY += height;
                }
                startX += width;
            }
        } else {
            // Draw single background with parallax effect
            float x = screenLeft - (cameraX * scrollSpeedX);
            float y = screenTop - (cameraY * scrollSpeedY);

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y + height);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + height);
            GL11.glEnd();
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public void cleanup() {
        if (textureId != -1) {
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }
    }
}