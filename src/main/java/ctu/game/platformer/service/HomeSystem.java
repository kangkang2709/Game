package ctu.game.platformer.service;

import ctu.game.platformer.controller.InputController;
import ctu.game.platformer.util.ResourceLoader;
import ctu.game.platformer.util.TextRenderer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ctu.game.platformer.model.common.GameState;
import jakarta.annotation.PostConstruct;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Service
public class HomeSystem implements InputController.MouseClickListener, InputController.MouseMoveListener {
    // Add these fields
    private int titleTextureId;
    private int[] menuTextureIds;
    private int[] menuSelectedTextureIds;

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
        menuTextureIds = new int[menuOptions.length];
        menuSelectedTextureIds = new int[menuOptions.length];
        for (int i = 0; i < menuOptions.length; i++) {
            menuTextureIds[i] = -1;
            menuSelectedTextureIds[i] = -1;
        }



    }
    private boolean texturesLoaded = false;

    private void loadTextures() {
        // Initialize arrays first
        menuTextureIds = new int[menuOptions.length];
        menuSelectedTextureIds = new int[menuOptions.length];

        try {
            // Load the title texture with fallback
            titleTextureId = resourceLoader.loadTextureFromFile("title.png");
            if (titleTextureId == -1) {
                titleTextureId = createFallbackTexture(255, 215, 0); // Gold color for title
                System.err.println("Using fallback texture for title");
            }

            for (int i = 0; i < menuOptions.length; i++) {
                try {
                    // Regular menu items
                    menuTextureIds[i] = resourceLoader.loadTextureFromFile("menu_" + i + ".png");
                    if (menuTextureIds[i] == -1) {
                        menuTextureIds[i] = createFallbackTexture(100, 100, 200); // Blue
                        System.err.println("Using fallback texture for menu_" + i + ".png");
                    }

                    // Selected menu items
                    menuSelectedTextureIds[i] = resourceLoader.loadTextureFromFile("menu_" + i + "_selected.png");
                    if (menuSelectedTextureIds[i] == -1) {
                        menuSelectedTextureIds[i] = createFallbackTexture(200, 100, 100); // Red
                        System.err.println("Using fallback texture for menu_" + i + "_selected.png");
                    }
                } catch (Exception e) {
                    // Create fallbacks for this menu item if anything goes wrong
                    System.err.println("Error loading textures for menu item " + i + ": " + e.getMessage());
                    if (menuTextureIds[i] <= 0) menuTextureIds[i] = createFallbackTexture(100, 100, 200);
                    if (menuSelectedTextureIds[i] <= 0) menuSelectedTextureIds[i] = createFallbackTexture(200, 100, 100);
                }
            }
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

            // Check if mouse is over this menu item
            if (mouseX >= 400 - width / 2 && mouseX <= 400 + width / 2 &&
                    mouseY >= y - height / 2 && mouseY <= y + height / 2) {
                selectedOption = i;
                overAnyItem = true;
                break;
            }
        }
    }

    public void render() {
        // Clear and set background color
        if (!texturesLoaded) {
            loadTextures();
            texturesLoaded = true;
        }
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Draw title
        drawText("FLATFORMER", 400, 150, true);

        // Draw menu options
        int startY = 250;
        int spacing = 60;

        for (int i = 0; i < menuOptions.length; i++) {
            int y = startY + i * spacing;
            boolean isSelected = (i == selectedOption);

            // Draw menu item
            drawMenuItem(menuOptions[i], 400, y, isSelected);
        }
    }

    private void drawMenuItem(String text, int x, int y, boolean isSelected) {
        int width = 200;
        int height = 40;

        // Find index of this menu item
        int index = -1;
        for (int i = 0; i < menuOptions.length; i++) {
            if (menuOptions[i].equals(text)) {
                index = i;
                break;
            }
        }

        if (index == -1) return; // Not found

        // Enable texturing and alpha blending
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Bind the appropriate texture for this menu item
        int textureId = isSelected ? menuSelectedTextureIds[index] : menuTextureIds[index];
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Set color to white for non-tinted rendering
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw the quad with texture coordinates
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex2f(x - width / 2, y - height / 2);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex2f(x + width / 2, y - height / 2);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex2f(x + width / 2, y + height / 2);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex2f(x - width / 2, y + height / 2);
        GL11.glEnd();

        // Disable states
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void drawText(String text, int x, int y, boolean isTitle) {
        // For the title, use the title texture
        int textureID = isTitle ? titleTextureId : -1;

        // If the texture is invalid, draw a colored rectangle instead
        if (textureID <= 0) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // Calculate dimensions
            int width = isTitle ? 300 : 180;
            int height = isTitle ? 50 : 30;

            // Use a gold color for the title
            if (isTitle) {
                GL11.glColor4f(1.0f, 0.84f, 0.0f, 1.0f); // Gold color
            } else {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // White
            }

            // Draw a rectangle
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x - width / 2, y - height / 2);
            GL11.glVertex2f(x + width / 2, y - height / 2);
            GL11.glVertex2f(x + width / 2, y + height / 2);
            GL11.glVertex2f(x - width / 2, y + height / 2);
            GL11.glEnd();

            GL11.glDisable(GL11.GL_BLEND);
            return;
        }

        // Calculate dimensions
        int width = isTitle ? 300 : 180;
        int height = isTitle ? 50 : 30;

        // Enable texturing and alpha blending
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Bind the texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        // Set color to white for non-tinted rendering
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw the quad with texture coordinates
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex2f(x - width / 2, y - height / 2);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex2f(x + width / 2, y - height / 2);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex2f(x + width / 2, y + height / 2);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex2f(x - width / 2, y + height / 2);
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
    public void cleanup() {
        try {
            // Delete title texture if valid
            if (titleTextureId > 0) {
                GL11.glDeleteTextures(titleTextureId);
            }

            // Delete menu textures if they exist
            if (menuTextureIds != null) {
                for (int textureId : menuTextureIds) {
                    if (textureId > 0) {
                        GL11.glDeleteTextures(textureId);
                    }
                }
            }

            if (menuSelectedTextureIds != null) {
                for (int textureId : menuSelectedTextureIds) {
                    if (textureId > 0) {
                        GL11.glDeleteTextures(textureId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in cleanup: " + e.getMessage());
        }
    }

    private void executeMenuAction() {
        System.out.println("Selected option: " + selectedOption);
        switch (selectedOption) {
            case 0:
                System.out.println("Switching to PLATFORM state...");
                gameStateManager.switchState(GameState.PLATFORM);
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

                // Check if mouse is over this menu item
                if (mouseX >= 400 - width / 2 && mouseX <= 400 + width / 2 &&
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