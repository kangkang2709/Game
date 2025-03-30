package ctu.game.platformer.service;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ctu.game.platformer.controller.InputController;

@Service
public class GameEngine {

    private final GameStateManager gameStateManager;
    private final InputController inputController;
    private final int windowWidth;
    private final int windowHeight;
    private final double targetFps;
    private final double updateRate;
    private final String windowTitle;
    private final boolean vSync;

    private long window;
    private boolean running = false;
    private double lastTime;

    @Autowired
    public GameEngine(@Lazy GameStateManager gameStateManager, InputController inputController,
                      int windowWidth, int windowHeight, String windowTitle, boolean vSync, double targetFps, double updateRate) {
        this.gameStateManager = gameStateManager;
        this.inputController = inputController;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.windowTitle = windowTitle;
        this.targetFps = targetFps;
        this.updateRate = updateRate;
        this.vSync = vSync;
    }

    public void start() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // 1. Thiết lập window hints TRƯỚC khi tạo window
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        // 2. Tạo window
        window = GLFW.glfwCreateWindow(windowWidth, windowHeight, windowTitle, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // 3. Thiết lập key callback
        inputController.initialize(window);

//        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) ->
//                inputController.invoke(window, key, scancode, action, mods)
//        );




        // 4. Make context current (phải làm TRƯỚC khi thiết lập VSync)
        GLFW.glfwMakeContextCurrent(window);

        // 5. Thiết lập VSync (phải làm SAU khi context đã current)
        if (vSync) {
            System.out.println("VSync enabled");
            GLFW.glfwSwapInterval(1);
        } else {
            System.out.println("VSync disabled");
            GLFW.glfwSwapInterval(0);
        }

        // 6. Hiển thị window (làm SAU khi đã thiết lập xong)
        GLFW.glfwShowWindow(window);

        // 7. Khởi tạo OpenGL
        GL.createCapabilities();
        GL11.glClearColor(0.2f, 0.3f, 0.3f, 1.0f);

        // 8. Thiết lập OpenGL
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        running = true;
        lastTime = GLFW.glfwGetTime();
    }

    // In GameEngine.java - modify the loop method

    private void loop() {
        final double TARGET_FPS = targetFps;
        final double UPDATE_RATE = updateRate;
        final double FRAME_TIME = 1_000_000_000.0 / TARGET_FPS;
        final double UPDATE_TIME = 1_000_000_000.0 / UPDATE_RATE;

        long lastRenderTime = System.nanoTime();
        long lastUpdateTime = System.nanoTime();
        long timer = System.currentTimeMillis();
        double deltaUpdate = 0;
        double deltaRender = 0;
        int frames = 0;
        int updates = 0;

        boolean wasFocused = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;

        while (running && !GLFW.glfwWindowShouldClose(window)) {
            long now = System.nanoTime();

            // Calculate time since last update and render
            deltaUpdate += (now - lastUpdateTime) / UPDATE_TIME;
            deltaRender += (now - lastRenderTime) / FRAME_TIME;

            lastUpdateTime = now;
            lastRenderTime = now;

            // Poll events regardless of update/render cycle
            GLFW.glfwPollEvents();

            // Handle focus changes
            boolean isFocused = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
            if (isFocused != wasFocused) {
                if (isFocused) System.out.println("Window regained focus");
                else System.out.println("Window lost focus");
                wasFocused = isFocused;
            }

            // Perform game logic updates at fixed UPDATE_RATE
            while (deltaUpdate >= 1) {
                inputController.update();
                gameStateManager.update();


                updates++;
                deltaUpdate--;
            }

            // Render at TARGET_FPS rate
            if (deltaRender >= 1) {
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                gameStateManager.render();

                GLFW.glfwSwapBuffers(window);
                frames++;
                deltaRender--;
            }

            // Log FPS and updates every second
            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                System.out.println("FPS: " + frames + " | Updates: " + updates);
                frames = 0;
                updates = 0;
            }

            // Sleep only if not using vsync
            if (!vSync) {
                long sleepTime = (long) ((lastRenderTime + FRAME_TIME - System.nanoTime()) / 1_000_000);
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }



    private void cleanup() {
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    public void stop() {
        running = false;
    }

    public long getWindow() {
        return window;
    }
}