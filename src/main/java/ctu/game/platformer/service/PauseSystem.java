package ctu.game.platformer.service;

import ctu.game.platformer.controller.InputController;
import ctu.game.platformer.util.ResourceLoader;
import ctu.game.platformer.util.TextRendererUtil;
import jakarta.annotation.PreDestroy;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ctu.game.platformer.model.common.GameState;

import java.nio.ByteBuffer;

@Service
public class PauseSystem implements InputController.KeyEventListener, InputController.MouseClickListener, InputController.MouseMoveListener {

    private final GameStateManager gameStateManager;
    private final InputController inputController;
    private final ResourceLoader resourceLoader;

    private GameState previousState;
    private int selectedOption = 0;
    private String[] menuOptions = {
            "Resume Game",
            "Options",
            "Back to Main Menu",
            "Exit Game"
    };

    private double mouseX, mouseY;
    private int drawX = 400; // Horizontal center for menu items
    private int backgroundTextureId;
    private boolean texturesLoaded = false;

    @Autowired
    public PauseSystem(@Lazy GameStateManager gameStateManager, InputController inputController, ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.gameStateManager = gameStateManager;
        this.inputController = inputController;
    }

    public void init() {
        // Register as listeners
        inputController.registerMouseMoveListener(this);
        inputController.registerMouseClickListener(this);
    }

    public void pause(GameState fromState) {
        previousState = fromState;
        selectedOption = 0;
        gameStateManager.switchState(GameState.PAUSE);

        // Load textures if not already loaded
        if (!texturesLoaded) {
            loadTextures();
        }
    }

    private void loadTextures() {
        try {
            // Load the background texture
            backgroundTextureId = resourceLoader.loadTextureFromFile("background.png");
            if (backgroundTextureId == -1) {
                backgroundTextureId = createFallbackTexture(20, 20, 40); // Dark overlay
                System.err.println("Using fallback texture for pause background");
            }
            texturesLoaded = true;
        } catch (Exception e) {
            System.err.println("Error loading pause menu textures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Create a simple texture for fallback
    private int createFallbackTexture(int r, int g, int b) {
        int textureID = -1;
        try {
            textureID = GL11.glGenTextures();
            if (textureID == 0) {
                return -1;
            }

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

            ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 4);
            buffer.order(java.nio.ByteOrder.nativeOrder());

            for (int i = 0; i < 4; i++) {
                buffer.put((byte) r);    // R
                buffer.put((byte) g);    // G
                buffer.put((byte) b);    // B
                buffer.put((byte) 180);  // Semi-transparent A
            }
            buffer.flip();

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                    2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            return textureID;
        } catch (Exception e) {
            System.err.println("Error creating fallback texture: " + e.getMessage());
            if (textureID > 0) {
                GL11.glDeleteTextures(textureID);
            }
            return -1;
        }
    }

    public void update() {
        // Check if mouse is over menu items
        checkMouseOverMenuItems();
    }

    private void checkMouseOverMenuItems() {
        int startY = 250;
        int spacing = 60;
        int width = 200;
        int height = 40;

        for (int i = 0; i < menuOptions.length; i++) {
            int y = startY + i * spacing;

            if (mouseX >= drawX - width / 2 && mouseX <= drawX + width / 2 &&
                    mouseY >= y - height / 2 && mouseY <= y + height / 2) {
                selectedOption = i;
                break;
            }
        }
    }

    public void render() {
        // Draw a semi-transparent overlay
        drawBackground();

        // Draw pause menu title
        TextRendererUtil.drawText("PAUSED", 400, 150, 36, true, 0xFFFFFFFF);

        // Draw menu options
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

        // Set color with alpha for semi-transparency
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);

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

        if (key == GLFW.GLFW_KEY_UP) {
            selectedOption = (selectedOption > 0) ? selectedOption - 1 : menuOptions.length - 1;
        } else if (key == GLFW.GLFW_KEY_DOWN) {
            selectedOption = (selectedOption < menuOptions.length - 1) ? selectedOption + 1 : 0;
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            executeMenuAction();
        } else if (key == GLFW.GLFW_KEY_ESCAPE) {
            // Pressing escape again resumes the game
            gameStateManager.switchState(previousState);
        }
    }

    private void executeMenuAction() {
        System.out.println("Selected pause menu option: " + selectedOption);
        switch (selectedOption) {
            case 0: // Resume Game
                gameStateManager.switchState(previousState);
                break;
            case 1: // Options
                // Implement options menu if available
                break;
            case 2: // Back to Main Menu
                gameStateManager.switchState(GameState.HOME);
                break;
            case 3: // Exit Game
                System.out.println("Exiting game...");
                System.exit(0);
                break;
        }
    }

    @Override
    public void onKeyEvent(int key, int action) {
        // Only process key events when in PAUSE state
        if (gameStateManager.getCurrentState() == GameState.PAUSE) {
            handleInput(key, action);
        }
    }

    @Override
    public void onMouseClick(int button, int action) {
        // Only process mouse clicks in PAUSE state
        if (gameStateManager.getCurrentState() != GameState.PAUSE) {
            return;
        }

        // Check for left mouse button press
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            int startY = 250;
            int spacing = 60;
            int width = 200;
            int height = 40;

            boolean clickedMenuItem = false;

            for (int i = 0; i < menuOptions.length; i++) {
                int y = startY + i * spacing;

                // Check if mouse is over this menu item
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
        // Store mouse position
        this.mouseX = xpos;
        this.mouseY = ypos;
    }

    @PreDestroy
    public void cleanup() {
        releaseResources();
    }

    public void releaseResources() {
        if (texturesLoaded) {
            System.out.println("Releasing PauseSystem resources");

            if (backgroundTextureId > 0) {
                GL11.glDeleteTextures(backgroundTextureId);
                backgroundTextureId = -1;
            }

            texturesLoaded = false;
        }
    }
}