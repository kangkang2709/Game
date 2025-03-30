package ctu.game.platformer.service;

import ctu.game.platformer.controller.InputController;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ctu.game.platformer.model.common.GameState;
import ctu.game.platformer.model.platformer.Player;

@Service
public class PlatformerSystem {

    private final GameStateManager gameStateManager;
    private final Player player;
    private final InputController inputController;

    // Add debug counter to help detect input issues
    private int updateCounter = 0;

    @Autowired
    public PlatformerSystem(@Lazy GameStateManager gameStateManager, InputController inputController) {
        this.gameStateManager = gameStateManager;
        this.inputController = inputController;
        this.player = new Player(100, 100);
    }

    public void update() {
        // Print debug info occasionally to verify input system is working
        updateCounter++;
        if (updateCounter % 300 == 0) {
            System.out.println("PlatformerSystem update: " + updateCounter +
                    " W:" + inputController.isKeyPressed(GLFW.GLFW_KEY_W) +
                    " A:" + inputController.isKeyPressed(GLFW.GLFW_KEY_A) +
                    " S:" + inputController.isKeyPressed(GLFW.GLFW_KEY_S) +
                    " D:" + inputController.isKeyPressed(GLFW.GLFW_KEY_D));
        }

        // Update player physics
        player.update();
    }

    public void render() {
        // Set to white color
        GL11.glColor3f(1.0f, 1.0f, 1.0f);

        // Draw white square in center for reference
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(400-25, 300-25);
        GL11.glVertex2f(400+25, 300-25);
        GL11.glVertex2f(400+25, 300+25);
        GL11.glVertex2f(400-25, 300+25);
        GL11.glEnd();

        // Draw player
        GL11.glColor3f(1.0f, 0.0f, 0.0f); // Set player color to red
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(player.getX(), player.getY());
        GL11.glVertex2f(player.getX() + player.getWidth(), player.getY());
        GL11.glVertex2f(player.getX() + player.getWidth(), player.getY() + player.getHeight());
        GL11.glVertex2f(player.getX(), player.getY() + player.getHeight());
        GL11.glEnd();
    }

    public void handleInput(int key, int action) {
        // State change
        if (key == GLFW.GLFW_KEY_V && action == GLFW.GLFW_PRESS) {
            gameStateManager.switchState(GameState.VISUAL_NOVEL);
        }

        // Movement controls - set movement flags based on key press/release
        if (key == GLFW.GLFW_KEY_W) {
            player.setMovingUp(action == GLFW.GLFW_PRESS);
        }
        if (key == GLFW.GLFW_KEY_S) {
            player.setMovingDown(action == GLFW.GLFW_PRESS);
        }
        if (key == GLFW.GLFW_KEY_A) {
            player.setMovingLeft(action == GLFW.GLFW_PRESS);
        }
        if (key == GLFW.GLFW_KEY_D) {
            player.setMovingRight(action == GLFW.GLFW_PRESS);
        }
    }
}