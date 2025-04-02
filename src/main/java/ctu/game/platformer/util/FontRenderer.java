package ctu.game.platformer.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class FontRenderer {
    private ByteBuffer ttfBuffer;
    private STBTTBakedChar.Buffer cdata;
    private int fontTexture;
    private int fontSize;
    private int bitmapWidth = 512;
    private int bitmapHeight = 512;

    public FontRenderer() {
        this(24);
    }

    public FontRenderer(int fontSize) {
        this.fontSize = fontSize;
        loadFont("fonts/WinkySans-Black.ttf");
    }

    private void loadFont(String fontPath) {
        try {
            // Read TTF file into byte array
            InputStream is = getClass().getClassLoader().getResourceAsStream(fontPath);
            if (is == null) {
                System.err.println("Could not find font: " + fontPath);
                return;
            }

            byte[] fontData = is.readAllBytes();
            ttfBuffer = BufferUtils.createByteBuffer(fontData.length);
            ttfBuffer.put(fontData);
            ttfBuffer.flip();

            // Create bitmap for ASCII codepoints
            ByteBuffer bitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);
            cdata = STBTTBakedChar.malloc(96); // ASCII 32-127
            int result = STBTruetype.stbtt_BakeFontBitmap(
                    ttfBuffer,
                    fontSize,
                    bitmap,
                    bitmapWidth,
                    bitmapHeight,
                    32, // First character
                    cdata);

            if (result <= 0) {
                System.err.println("Failed to bake font bitmap");
                return;
            }

            // Create OpenGL texture from bitmap
            fontTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_ALPHA,
                    bitmapWidth,
                    bitmapHeight,
                    0,
                    GL11.GL_ALPHA,
                    GL11.GL_UNSIGNED_BYTE,
                    bitmap);

            // Set texture parameters
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        } catch (IOException e) {
            System.err.println("Failed to load font: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void renderText(String text, float x, float y, float scale, float r, float g, float b) {
        if (cdata == null) return;

        // Set up rendering
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, 1.0f);

        // Scale affects only position, not size
        float xpos = x;
        float ypos = y;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x0 = stack.floats(0);
            FloatBuffer y0 = stack.floats(0);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 32 || c > 127) c = '?'; // Replace non-ASCII chars with '?'

                STBTruetype.stbtt_GetBakedQuad(
                        cdata,
                        bitmapWidth, bitmapHeight,
                        c - 32, // Adjust for ASCII start
                        x0, y0,
                        q,
                        false);

                xpos = q.x0() * scale + x;
                ypos = q.y0() * scale + y;
                float x2 = q.x1() * scale + x;
                float y2 = q.y1() * scale + y;

                // Draw quad
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(q.s0(), q.t0()); GL11.glVertex2f(xpos, ypos);
                GL11.glTexCoord2f(q.s1(), q.t0()); GL11.glVertex2f(x2, ypos);
                GL11.glTexCoord2f(q.s1(), q.t1()); GL11.glVertex2f(x2, y2);
                GL11.glTexCoord2f(q.s0(), q.t1()); GL11.glVertex2f(xpos, y2);
                GL11.glEnd();

                // Advance cursor
                xpos = x2;
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public void renderWrappedText(String text, float x, float y, float maxWidth, float scale, float r, float g, float b) {
        List<String> lines = wrapText(text, maxWidth, scale);
        float lineHeight = fontSize * scale * 1.2f;

        for (int i = 0; i < lines.size(); i++) {
            renderText(lines.get(i), x, y + i * lineHeight, scale, r, g, b);
        }
    }

    public void renderCenteredText(String text, float x, float y, float scale, float r, float g, float b) {
        float width = calculateTextWidth(text, scale);
        renderText(text, x - width / 2, y, scale, r, g, b);
    }

    private float calculateTextWidth(String text, float scale) {
        if (cdata == null) return 0;

        float width = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x0 = stack.floats(0);
            FloatBuffer y0 = stack.floats(0);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 32 || c > 127) c = '?';

                // Only need to check final position
                STBTruetype.stbtt_GetBakedQuad(
                        cdata,
                        bitmapWidth, bitmapHeight,
                        c - 32,
                        x0, y0,
                        q,
                        false);
            }
            width = x0.get(0) * scale;
        }

        return width;
    }

    private List<String> wrapText(String text, float maxWidth, float scale) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        float currentWidth = 0;

        String[] words = text.split("\\s+");
        for (String word : words) {
            float wordWidth = calculateTextWidth(word + " ", scale);

            if (currentWidth + wordWidth > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word + " ");
                    currentWidth = wordWidth;
                } else {
                    // Word is too long for the line, split it
                    lines.add(word);
                    currentLine = new StringBuilder();
                    currentWidth = 0;
                }
            } else {
                currentLine.append(word).append(" ");
                currentWidth += wordWidth;
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    public void cleanup() {
        if (cdata != null) {
            cdata.free();
        }
        GL11.glDeleteTextures(fontTexture);
    }
}