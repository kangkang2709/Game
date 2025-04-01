package ctu.game.platformer.model.tilemap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

public class TextureManager {
    private static Map<String, Integer> textureCache = new HashMap<>();

    // Default texture for error cases
    private static int defaultTextureId = -1;

    public static int loadTexture(String path) {
        return loadTexture(path, false);
    }

    public static int loadTexture(String path, boolean useCache) {
        // Skip loading for null or empty paths
        if ("empty".equals(path)) {
            return -1;  // Return invalid texture ID for empty tiles
        }

        if (path == null || path.isEmpty()) {
            return -1;
        }

        // Check if texture is already in cache
        if (useCache && textureCache.containsKey(path)) {
            return textureCache.get(path);
        }

        int textureId = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Create texture ID
            textureId = GL11.glGenTextures();

            // Load image data
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Use direct buffer for image loading
            ByteBuffer imageBuffer = readResourceToByteBuffer(path);
            if (imageBuffer == null) {
                System.err.println("Failed to load texture: " + path);
                return getDefaultTexture();
            }

            // Flip Y so image isn't upside down
            STBImage.stbi_set_flip_vertically_on_load(false);

            ByteBuffer imageData = STBImage.stbi_load_from_memory(
                    imageBuffer,
                    width,
                    height,
                    channels,
                    4); // Force RGBA format

            if (imageData == null) {
                System.err.println("Failed to decode image: " + path + ", error: " + STBImage.stbi_failure_reason());
                return getDefaultTexture();
            }

            // Bind texture and set parameters
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            // Set texture parameters for better quality
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            // Upload texture data
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA,
                    width.get(0),
                    height.get(0),
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    imageData);

            // Free image memory
            STBImage.stbi_image_free(imageData);

            // Store in cache if caching is enabled
            if (useCache) {
                textureCache.put(path, textureId);
            }

            return textureId;
        } catch (Exception e) {
            System.err.println("Error loading texture " + path + ": " + e.getMessage());
            e.printStackTrace();

            // Delete failed texture
            if (textureId > 0) {
                GL11.glDeleteTextures(textureId);
            }

            // Return default texture on error
            return getDefaultTexture();
        }
    }

    private static ByteBuffer readResourceToByteBuffer(String resource) {
        try (InputStream is = TextureManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                System.err.println("Resource not found: " + resource);
                return null;
            }

            // Get initial size estimate
            int initialSize = Math.max(is.available(), 16 * 1024); // At least 16KB

            ByteBuffer buffer = BufferUtils.createByteBuffer(initialSize);
            ReadableByteChannel rbc = Channels.newChannel(is);

            while (true) {
                int bytesRead = rbc.read(buffer);
                if (bytesRead == -1) break;

                // If buffer is full but there's more data
                if (buffer.remaining() == 0) {
                    // Double the buffer size
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }

            buffer.flip();
            return buffer;
        } catch (IOException e) {
            System.err.println("Failed to read resource: " + resource);
            e.printStackTrace();
            return null;
        }
    }

    private static synchronized int getDefaultTexture() {
        // Create a simple default texture if not already created
        if (defaultTextureId == -1) {
            defaultTextureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, defaultTextureId);

            // Create a simple 2x2 checkerboard texture
            ByteBuffer pixels = BufferUtils.createByteBuffer(4 * 4);

            // Red and black checkerboard
            byte[] data = {
                    (byte)255, (byte)0, (byte)0, (byte)255,     // Red
                    (byte)0, (byte)0, (byte)0, (byte)255,       // Black
                    (byte)0, (byte)0, (byte)0, (byte)255,       // Black
                    (byte)255, (byte)0, (byte)0, (byte)255      // Red
            };
            pixels.put(data);
            pixels.flip();

            // Upload texture
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

            // Set texture parameters
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            // Add to cache to prevent recreation
            textureCache.put("__default__", defaultTextureId);
        }

        return defaultTextureId;
    }

    public static void clearCache() {
        for (Integer id : textureCache.values()) {
            GL11.glDeleteTextures(id);
        }
        textureCache.clear();
        defaultTextureId = -1;
    }

    public static int getCacheSize() {
        return textureCache.size();
    }

    public static boolean isCached(String path) {
        return textureCache.containsKey(path);
    }

}