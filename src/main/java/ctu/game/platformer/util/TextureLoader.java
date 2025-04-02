package ctu.game.platformer.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class TextureLoader {

    public static int loadTexture(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            InputStream is = TextureLoader.class.getClassLoader().getResourceAsStream(path);
            if (is == null) {
                System.err.println("Could not find texture: " + path);
                return 0;
            }

            byte[] imageBytes;
            try {
                imageBytes = is.readAllBytes();
            } catch (IOException e) {
                System.err.println("Failed to read texture data: " + path);
                return 0;
            }

            ByteBuffer imageBuffer = BufferUtils.createByteBuffer(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();

            ByteBuffer image = STBImage.stbi_load_from_memory(
                    imageBuffer, w, h, channels, 4);

            if (image == null) {
                System.err.println("Failed to decode texture: " + path);
                return 0;
            }

            // Create OpenGL texture
            int textureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

            // Set texture parameters
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            // Upload texture data
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w.get(), h.get(),
                    0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);

            // Free image memory
            STBImage.stbi_image_free(image);

            return textureID;
        } catch (Exception e) {
            System.err.println("Unexpected error loading texture: " + path);
            e.printStackTrace();
            return 0;
        }
    }
}