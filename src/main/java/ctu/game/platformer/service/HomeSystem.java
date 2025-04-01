package ctu.game.platformer.service;

import ctu.game.platformer.controller.InputController;
import ctu.game.platformer.util.AudioManager;
import ctu.game.platformer.util.ResourceLoader;
import ctu.game.platformer.util.TextRendererUtil;
import jakarta.annotation.PreDestroy;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ctu.game.platformer.model.common.GameState;
import jakarta.annotation.PostConstruct;

import java.nio.ByteBuffer;

@Service
public class HomeSystem implements InputController.MouseClickListener, InputController.MouseMoveListener {
    // Add these fields
    private int titleTextureId;
    private int[] menuTextureIds;
    private int[] menuSelectedTextureIds;
    private int backgroundTextureId;

    int drawX =200;

    @Autowired
    private AudioManager audioManager;

    private final GameStateManager gameStateManager;
    private final InputController inputController;

    private final ResourceLoader resourceLoader;
    // Menu options
    private final String[] menuOptions = {
            "Start New Game",
            "Continue Game",
            "Load Save",
            "Exit Game"
    };


    private int selectedOption = 0;
    private double mouseX, mouseY;

    @Autowired
    public HomeSystem(@Lazy GameStateManager gameStateManager, InputController inputController, ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.gameStateManager = gameStateManager;
        this.inputController = inputController;
    }

    @PostConstruct
    private void init() {
        // Register for mouse events

        inputController.registerMouseMoveListener(this);
        inputController.registerMouseClickListener(this);

        // We'll initialize them on first render
        titleTextureId = -1;
        backgroundTextureId = -1;
        menuTextureIds = new int[menuOptions.length];
        menuSelectedTextureIds = new int[menuOptions.length];
        for (int i = 0; i < menuOptions.length; i++) {
            menuTextureIds[i] = -1;
            menuSelectedTextureIds[i] = -1;
        }

//        audioManager.initialize();
//        audioManager.loadBackgroundMusic("background.wav");
//        audioManager.playBackgroundMusic();
//        audioManager.setVolume(0.3f); // 0.0 (silent) to 1.0 (full)

    }
    private boolean texturesLoaded = false;
    @PreDestroy
    private void cleanup() {
        releaseResources();
    }
    private void releaseResources() {
        if (texturesLoaded) {
            System.out.println("Releasing HomeSystem resources");

            // Stop audio
            audioManager.stopBackgroundMusic();

            // Delete textures
            if (backgroundTextureId > 0) {
                GL11.glDeleteTextures(backgroundTextureId);
                backgroundTextureId = -1;
            }

            if (titleTextureId > 0) {
                GL11.glDeleteTextures(titleTextureId);
                titleTextureId = -1;
            }

            // Delete menu textures
            for (int i = 0; i < menuTextureIds.length; i++) {
                if (menuTextureIds[i] > 0) {
                    GL11.glDeleteTextures(menuTextureIds[i]);
                    menuTextureIds[i] = -1;
                }

                if (menuSelectedTextureIds[i] > 0) {
                    GL11.glDeleteTextures(menuSelectedTextureIds[i]);
                    menuSelectedTextureIds[i] = -1;
                }
            }

            // Reset state
            texturesLoaded = false;
        }
    }

    private void loadTextures() {
        // Initialize arrays first
        menuTextureIds = new int[menuOptions.length];
        menuSelectedTextureIds = new int[menuOptions.length];

        try {
            // Load the background texture
            backgroundTextureId = resourceLoader.loadTextureFromFile("background.png");
            if (backgroundTextureId == -1) {
                backgroundTextureId = createFallbackTexture(30, 30, 60); // Dark blue background
                System.err.println("Using fallback texture for background");
            }

            // Load the title texture with fallback
//            titleTextureId = resourceLoader.loadTextureFromFile("title.png");
//            if (titleTextureId == -1) {
//                titleTextureId = createFallbackTexture(255, 215, 0); // Gold color for title
//                System.err.println("Using fallback texture for title");
//            }

            // Rest of your existing texture loading code...
        } catch (Exception e) {
            System.err.println("Critical error loading textures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Improved fallback texture creation with custom colors
    private int createFallbackTexture(int r, int g, int b) {
        int textureID = -1;
        try {
            // Generate texture ID
            textureID = GL11.glGenTextures();
            if (textureID == 0) {
                System.err.println("Failed to generate texture ID");
                return -1;
            }

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

            // Create a proper direct ByteBuffer (4 pixels, 4 bytes per pixel)
            ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 4);
            buffer.order(java.nio.ByteOrder.nativeOrder()); // IMPORTANT: Use native byte order

            // Fill with color data (2x2 pixels)
            for (int i = 0; i < 4; i++) {
                buffer.put((byte) r);    // R
                buffer.put((byte) g);    // G
                buffer.put((byte) b);    // B
                buffer.put((byte) 255);  // A
            }
            buffer.flip(); // Important: prepare for reading

            // Set texture parameters with error checking
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            // Create the texture (2x2 pixels)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                    2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            // Check for OpenGL errors
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) {
                System.err.println("OpenGL error in createFallbackTexture: " + error);
                GL11.glDeleteTextures(textureID);
                return -1;
            }

            return textureID;
        } catch (Exception e) {
            System.err.println("Error creating fallback texture: " + e.getMessage());
            e.printStackTrace();
            if (textureID > 0) {
                GL11.glDeleteTextures(textureID);
            }
            return -1;
        }
    }

    public void update() {
        // Update the selected option based on mouse position
        checkMouseOverMenuItems();
    }

    private void checkMouseOverMenuItems() {
        int startY = 250;
        int spacing = 60;
        int width = 200;
        int height = 40;

        // Reset selection if not over any menu item
        boolean overAnyItem = false;

        for (int i = 0; i < menuOptions.length; i++) {
            int y = startY + i * spacing;

            // Update to use drawX instead of hardcoded 400
            if (mouseX >= drawX - width / 2 && mouseX <= drawX + width / 2 &&
                    mouseY >= y - height / 2 && mouseY <= y + height / 2) {
                selectedOption = i;
                overAnyItem = true;
                break;
            }
        }
    }

    public void render() {
        // Check if textures are loaded
        if (!texturesLoaded) {
            loadTextures();
            texturesLoaded = true;
        }

        // Draw background
        drawBackground();

        // Draw title using TextRendererUtil
        TextRendererUtil.drawText("FLATFORMER", 400, 150, 32, true);

        // Draw menu options using TextRendererUtil
        int startY = 250;
        int spacing = 60;

        for (int i = 0; i < menuOptions.length; i++) {
            int y = startY + i * spacing;
            boolean isSelected = (i == selectedOption);

            // Draw menu text with selection highlight
            float fontSize = isSelected ? 24.0f : 20.0f;
            int color = isSelected ? 0xFFFFAA00 : 0xFFFFFFFF; // Orange for selected, white for normal

            TextRendererUtil.drawText(menuOptions[i], drawX, y, fontSize, true, color);
        }
    }

    private void drawBackground() {
        // Enable texturing and alpha blending
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Bind the background texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, backgroundTextureId);

        // Set color to white (no tint)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw the full screen quad
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex2f(800, 0);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex2f(800, 600);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex2f(0, 600);
        GL11.glEnd();

        // Disable states
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }



    public void handleInput(int key, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        System.out.println("Key Pressed: " + key);

        if (key == GLFW.GLFW_KEY_UP) {
            selectedOption = (selectedOption > 0) ? selectedOption - 1 : menuOptions.length - 1;
        } else if (key == GLFW.GLFW_KEY_DOWN) {
            selectedOption = (selectedOption < menuOptions.length - 1) ? selectedOption + 1 : 0;
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            // Xử lý chọn option khi nhấn Enter
            executeMenuAction();
        }

        System.out.println("Selected option: " + selectedOption);
    }

    private void executeMenuAction() {
        System.out.println("Selected option: " + selectedOption);
        switch (selectedOption) {
            case 0:
                System.out.println("Switching to PLATFORM state...");
                gameStateManager.switchState(GameState.PLATFORM);
                releaseResources();
                break;
            case 1:
                System.out.println("Switching to PLATFORM state...");
                gameStateManager.switchState(GameState.PLATFORM);
                break;
            case 2:
                System.out.println("Switching to PLATFORM state...");
                gameStateManager.switchState(GameState.PLATFORM);
                break;
            case 3:
                System.out.println("Exiting game...");
                System.exit(0);
                break;
        }
    }


    @Override
    public void onMouseClick(int button, int action) {


        // Only process mouse clicks in HOME state
        if (gameStateManager.getCurrentState() != GameState.HOME) {
            return;
        }

        // Check for left mouse button press
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            // Check if mouse is actually over a menu item before executing
            int startY = 250;
            int spacing = 60;
            int width = 200;
            int height = 40;

            boolean clickedMenuItem = false;

            for (int i = 0; i < menuOptions.length; i++) {
                int y = startY + i * spacing;

                // Check if mouse is over this menu item, now using drawX
                // Update to use drawX instead of hard-coded 400
                if (mouseX >= drawX - width / 2 && mouseX <= drawX + width / 2 &&
                        mouseY >= y - height / 2 && mouseY <= y + height / 2) {
                    selectedOption = i;
                    clickedMenuItem = true;
                    break;
                }
            }

            // Only execute the menu action if a menu item was clicked
            if (clickedMenuItem) {
                executeMenuAction();
            }
        }
    }

    @Override
    public void onMouseMove(double xpos, double ypos) {
        // Store mouse position for use in update method
        this.mouseX = xpos;
        this.mouseY = ypos;
    }
}