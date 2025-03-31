package ctu.game.platformer.util;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

@Component
public class AudioManager {
    private long device;
    private long context;
    private final Map<String, Integer> soundBuffers = new HashMap<>();
    private int backgroundMusicSource = -1;

    public void initialize() {
        // Initialize OpenAL
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to open the default OpenAL device");
        }

        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        if (context == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create OpenAL context");
        }

        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));
    }

    public void loadBackgroundMusic(String filename) {
        try {
            int buffer = loadAudioFile("assets/audio/music/" + filename);
            backgroundMusicSource = AL10.alGenSources();

            // Configure the source
            AL10.alSourcei(backgroundMusicSource, AL10.AL_BUFFER, buffer);
            AL10.alSourcei(backgroundMusicSource, AL10.AL_LOOPING, AL10.AL_TRUE);
            AL10.alSourcef(backgroundMusicSource, AL10.AL_GAIN, 0.5f); // Volume at 50%
        } catch (Exception e) {
            System.err.println("Failed to load background music: " + e.getMessage());
        }
    }

    public void playBackgroundMusic() {
        if (backgroundMusicSource != -1) {
            AL10.alSourcePlay(backgroundMusicSource);
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusicSource != -1) {
            AL10.alSourceStop(backgroundMusicSource);
        }
    }

    public void setVolume(float volume) {
        if (backgroundMusicSource != -1) {
            AL10.alSourcef(backgroundMusicSource, AL10.AL_GAIN, volume);
        }
    }

    private int loadAudioFile(String filename) throws Exception {
        if (soundBuffers.containsKey(filename)) {
            return soundBuffers.get(filename);
        }

        InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);
        if (stream == null) {
            throw new RuntimeException("Audio file not found: " + filename);
        }

        AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(stream));
        AudioFormat format = audioStream.getFormat();

        // Read the audio data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = audioStream.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, read);
        }
        byte[] audioData = baos.toByteArray();

        // Determine the OpenAL format
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int openALFormat;

        if (channels == 1) {
            openALFormat = bitsPerSample == 8 ? AL10.AL_FORMAT_MONO8 : AL10.AL_FORMAT_MONO16;
        } else {
            openALFormat = bitsPerSample == 8 ? AL10.AL_FORMAT_STEREO8 : AL10.AL_FORMAT_STEREO16;
        }

        // Create the buffer
        int alBuffer = AL10.alGenBuffers();
        ByteBuffer audioBuffer = ByteBuffer.allocateDirect(audioData.length);
        audioBuffer.put(audioData);
        audioBuffer.flip();

        // Load the audio data into the buffer
        AL10.alBufferData(alBuffer, openALFormat, audioBuffer, (int)format.getSampleRate());

        soundBuffers.put(filename, alBuffer);
        return alBuffer;
    }

    public void cleanup() {
        if (backgroundMusicSource != -1) {
            AL10.alSourceStop(backgroundMusicSource);
            AL10.alDeleteSources(backgroundMusicSource);
        }

        for (Integer buffer : soundBuffers.values()) {
            AL10.alDeleteBuffers(buffer);
        }

        if (context != MemoryUtil.NULL) {
            ALC10.alcDestroyContext(context);
        }

        if (device != MemoryUtil.NULL) {
            ALC10.alcCloseDevice(device);
        }
    }
}