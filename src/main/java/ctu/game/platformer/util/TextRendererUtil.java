package ctu.game.platformer.util;

import org.lwjgl.opengl.GL11;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for rendering text in the game
 */
@Component
public class TextRendererUtil {
    private static boolean initialized = false;
    private static int fontTextureId = -1;
    private static final Map<Character, CharInfo> charMap = new HashMap<>();

    private static class CharInfo {
        float u1, v1; // Top-left texture coordinates
        float u2, v2; // Bottom-right texture coordinates
        int width;    // Character width
    }

    private static void initialize() {
        if (initialized) return;

        try {
            // Create a font texture atlas
            int fontSize = 24;
            Font font = new Font("Arial", Font.PLAIN, fontSize);

            // Create texture atlas size
            int textureWidth = 512;
            int textureHeight = 512;

            // Create a buffered image for the font texture atlas
            BufferedImage image = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();

            // Set rendering hints for better quality
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Set background transparent
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, textureWidth, textureHeight);

            // Set font color to white
            g.setColor(Color.WHITE);
            g.setFont(font);

            // Metrics for measuring text
            FontMetrics metrics = g.getFontMetrics();
            int charHeight = metrics.getHeight();

            // Draw characters to the texture atlas and store their info
            int x = 0;
            int y = 0;

            // ASCII range for basic characters
            for (int i = 32; i < 127; i++) {
                char c = (char) i;
                String charStr = String.valueOf(c);
                int charWidth = metrics.stringWidth(charStr);

                // Check if we need to go to next row
                if (x + charWidth >= textureWidth) {
                    x = 0;
                    y += charHeight + 1;

                    // Check if we've run out of texture space
                    if (y + charHeight >= textureHeight) {
                        break;
                    }
                }

                // Draw character
                g.drawString(charStr, x, y + metrics.getAscent());

                // Store character info
                CharInfo charInfo = new CharInfo();
                charInfo.u1 = (float) x / textureWidth;
                charInfo.v1 = (float) y / textureHeight;
                charInfo.u2 = (float) (x + charWidth) / textureWidth;
                charInfo.v2 = (float) (y + charHeight) / textureHeight;
                charInfo.width = charWidth;

                charMap.put(c, charInfo);

                // Move to next position
                x += charWidth + 1;
            }

            g.dispose();

            // Create OpenGL texture from the image
            fontTextureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);

            // Set texture parameters
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            // Get image pixel data
            int[] pixels = new int[textureWidth * textureHeight];
            image.getRGB(0, 0, textureWidth, textureHeight, pixels, 0, textureWidth);

            // Convert to RGBA byte buffer
            ByteBuffer buffer = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
            buffer.flip();

            // Upload texture data
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, textureWidth, textureHeight,
                    0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            initialized = true;

        } catch (Exception e) {
            System.err.println("Error initializing font renderer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Draw text with default white color
     *
     * @param text The text to draw
     * @param x X position on screen
     * @param y Y position on screen
     * @param fontSize Size of the font
     * @param centered Whether to center the text horizontally
     */
    public static void drawText(String text, float x, float y, float fontSize, boolean centered) {
        drawText(text, x, y, fontSize, centered, 0xFFFFFFFF); // White text by default
    }

    /**
     * Draw text with specified color
     *
     * @param text The text to draw
     * @param x X position on screen
     * @param y Y position on screen
     * @param fontSize Size of the font
     * @param centered Whether to center the text horizontally
     * @param color RGBA color value (0xAARRGGBB format)
     */
    public static void drawText(String text, float x, float y, float fontSize, boolean centered, int color) {
        if (text == null || text.isEmpty()) return;

        // Initialize if needed
        if (!initialized) {
            initialize();
        }

        // If initialization failed or we don't have a texture, fall back to simple rendering
        if (!initialized || fontTextureId <= 0) {
            drawSimpleText(text, x, y, fontSize, centered, color);
            return;
        }

        // Extract color components
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Calculate text width for centering
        float textWidth = 0;
        float scale = fontSize / 24.0f; // Our base font size is 24

        for (char c : text.toCharArray()) {
            CharInfo charInfo = charMap.getOrDefault(c, charMap.get(' '));
            if (charInfo != null) {
                textWidth += charInfo.width * scale;
            }
        }

        // Calculate starting position
        float startX = centered ? x - textWidth / 2 : x;
        float posX = startX;

        // Enable texturing and blending
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Bind texture and set color
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);
        GL11.glColor4f(r, g, b, a);

        // Draw each character
        GL11.glBegin(GL11.GL_QUADS);
        for (char c : text.toCharArray()) {
            CharInfo charInfo = charMap.getOrDefault(c, charMap.get(' '));
            if (charInfo == null) continue;

            float charWidth = charInfo.width * scale;
            float charHeight = 24 * scale; // Base height is 24

            // Draw character quad with texture coordinates
            GL11.glTexCoord2f(charInfo.u1, charInfo.v1);
            GL11.glVertex2f(posX, y - charHeight/2);

            GL11.glTexCoord2f(charInfo.u2, charInfo.v1);
            GL11.glVertex2f(posX + charWidth, y - charHeight/2);

            GL11.glTexCoord2f(charInfo.u2, charInfo.v2);
            GL11.glVertex2f(posX + charWidth, y + charHeight/2);

            GL11.glTexCoord2f(charInfo.u1, charInfo.v2);
            GL11.glVertex2f(posX, y + charHeight/2);

            // Advance position
            posX += charWidth;
        }
        GL11.glEnd();

        // Reset color and disable states
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /**
     * Simple fallback text rendering method
     */
    private static void drawSimpleText(String text, float x, float y, float fontSize, boolean centered, int color) {
        // Extract color components
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Calculate width for centering
        float width = text.length() * fontSize * 0.5f; // Approximate width
        float startX = centered ? x - width / 2 : x;

        // Disable texturing for solid color rendering
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Set text color
        GL11.glColor4f(r, g, b, a);

        // Character rendering parameters
        float charWidth = fontSize * 0.6f;
        float charHeight = fontSize;
        float spacing = fontSize * 0.1f;

        // Draw each character
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float charX = startX + i * (charWidth + spacing);

            // Draw a simple rectangle for each character
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(charX, y - charHeight/2);
            GL11.glVertex2f(charX + charWidth, y - charHeight/2);
            GL11.glVertex2f(charX + charWidth, y + charHeight/2);
            GL11.glVertex2f(charX, y + charHeight/2);
            GL11.glEnd();
        }

        // Reset color and disable blend
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Clean up resources
     */
    public static void cleanup() {
        if (fontTextureId > 0) {
            GL11.glDeleteTextures(fontTextureId);
            fontTextureId = -1;
        }
        initialized = false;
        charMap.clear();
    }
}