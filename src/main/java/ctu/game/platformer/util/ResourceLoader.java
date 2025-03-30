// src/main/java/ctu/game/flatformer/util/ResourceLoader.java
package ctu.game.platformer.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

@Component
public class ResourceLoader {

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