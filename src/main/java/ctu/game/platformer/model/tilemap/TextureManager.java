package ctu.game.platformer.model.tilemap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class TextureManager {
    private static final Map<String, Integer> textureMap = new HashMap<>();

    public static int loadTexture(String path) {
        if (textureMap.containsKey(path)) {
            return textureMap.get(path);
        }

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            InputStream inputStream = TextureManager.class.getClassLoader().getResourceAsStream(path);
            if (inputStream == null) {
                System.err.println("Failed to load texture: " + path + " (File not found in classpath)");
                generateFallbackTexture();
            } else {
                byte[] imageBytes = inputStream.readAllBytes();
                ByteBuffer imageBuffer = ByteBuffer.allocateDirect(imageBytes.length);
                imageBuffer.put(imageBytes).flip();

                ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);

                if (image == null) {
                    System.err.println("Failed to load texture: " + path + ", reason: " + STBImage.stbi_failure_reason());
                    generateFallbackTexture();
                } else {
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(0), height.get(0), 0,
                            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
                    STBImage.stbi_image_free(image);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading texture " + path + ": " + e.getMessage());
            generateFallbackTexture();
        }

        textureMap.put(path, textureId);
        return textureId;
    }

    private static void generateFallbackTexture() {
        ByteBuffer fallback = ByteBuffer.allocateDirect(4);
        fallback.put((byte) 255).put((byte) 0).put((byte) 255).put((byte) 255).flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, fallback);
    }

    public static void deleteTexture(String path) {
        Integer textureId = textureMap.remove(path);
        if (textureId != null) {
            GL11.glDeleteTextures(textureId);
        }
    }

    public static void deleteAllTextures() {
        for (Integer textureId : textureMap.values()) {
            GL11.glDeleteTextures(textureId);
        }
        textureMap.clear();
    }
}