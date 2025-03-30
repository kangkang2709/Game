// src/main/java/ctu/game/flatformer/service/VisualNovelSystem.java
package ctu.game.platformer.service;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ctu.game.platformer.model.common.GameState;
import ctu.game.platformer.model.visualnovel.Dialog;

@Service
public class VisualNovelSystem {



    private Dialog currentDialog;


    private final GameStateManager gameStateManager;
    // Other fields

    @Autowired
    public VisualNovelSystem(@Lazy GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
        // Initialize other fields
    }

    public void update() {
        // Update dialog and story progression
    }

    public void render() {
        // Draw background
        GL11.glColor3f(0.5f, 0.5f, 0.8f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(800, 0);
        GL11.glVertex2f(800, 600);
        GL11.glVertex2f(0, 600);
        GL11.glEnd();

        // Draw dialog box
        GL11.glColor3f(0.0f, 0.0f, 0.0f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(50, 400);
        GL11.glVertex2f(750, 400);
        GL11.glVertex2f(750, 550);
        GL11.glVertex2f(50, 550);
        GL11.glEnd();

        // In a real implementation, you would render text here
    }

    public void handleInput(int key, int action) {
        if (action == GLFW.GLFW_PRESS) {
            switch (key) {
                case GLFW.GLFW_KEY_SPACE:
                    // Advance dialog
                    break;
                case GLFW.GLFW_KEY_P:
                    gameStateManager.switchState(GameState.PLATFORM);
                    break;
            }
        }
    }
}