// src/main/java/ctu/game/flatformer/util/ResourceLoader.java
package ctu.game.platformer.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjgl.opengl.GL11;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;

@Component
public class ResourceLoader {


    public int loadTextureFromFile(String fileName) {
        try {
            String resourcePath = "assets/images/" + fileName;
            InputStream stream = this.loadResourceAsStream(resourcePath);

            if (stream == null) {
                System.err.println("ERROR: Could not find resource: " + resourcePath);
                return -1;
            }

            BufferedImage image = ImageIO.read(stream);

            int textureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

            ByteBuffer buffer = createByteBuffer(image);

            // Configure texture
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(),
                    0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            return textureID;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public ByteBuffer createByteBuffer(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
            buffer.put((byte) (pixel & 0xFF));         // Blue
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
        }
        buffer.flip();
        return buffer;
    }

    public InputStream loadResourceAsStream(String path) {
        return ResourceLoader.class.getClassLoader().getResourceAsStream(path);
    }

    public ByteBuffer loadResourceAsBuffer(String path) {
        ByteBuffer buffer = null;

        try {
            Path filePath = Paths.get(ResourceLoader.class.getClassLoader().getResource(path).toURI());
            buffer = loadPathAsBuffer(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buffer;
    }


    private ByteBuffer loadPathAsBuffer(Path path) throws IOException {
        ByteBuffer buffer;

        try (SeekableByteChannel channel = Files.newByteChannel(path)) {
            buffer = ByteBuffer.allocateDirect((int) channel.size() + 1);
            while (channel.read(buffer) != -1) {
                // Keep reading until EOF
            }
        }

        buffer.flip();
        return buffer;
    }

    public ByteBuffer loadStreamAsBuffer(InputStream stream) throws IOException {
        ByteBuffer buffer;

        try (ReadableByteChannel channel = Channels.newChannel(stream)) {
            buffer = ByteBuffer.allocateDirect(8 * 1024);
            while (channel.read(buffer) != -1) {
                if (buffer.remaining() == 0) {
                    buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                }
            }
        }

        buffer.flip();
        return buffer;
    }

    private ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}