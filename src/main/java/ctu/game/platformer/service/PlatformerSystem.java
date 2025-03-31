package ctu.game.platformer.service;

import ctu.game.platformer.controller.InputController;
import ctu.game.platformer.model.common.GameState;
import ctu.game.platformer.model.platformer.Player;
import ctu.game.platformer.model.tilemap.TileMap;
import ctu.game.platformer.util.AudioManager;
import jakarta.annotation.PostConstruct;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PlatformerSystem {
    private final GameStateManager gameStateManager;
    private final InputController inputController;
    private final Player player;
    private final TileMap tileMap;
    private final int screenWidth;
    private final int screenHeight;
    @Autowired
    private AudioManager audioManager;
    @Autowired
    public PlatformerSystem(
            @Lazy GameStateManager gameStateManager,
            InputController inputController,
            @Autowired(required = false) TileMap tileMap,
            @Autowired int windowWidth,
            @Autowired int windowHeight) {
        this.gameStateManager = gameStateManager;
        this.inputController = inputController;
        this.screenWidth = windowWidth;
        this.screenHeight = windowHeight;
        this.tileMap = tileMap != null ? tileMap : new TileMap();
        this.player = new Player(100, 100, 32, 64);

        this.player.setTileMap(this.tileMap);
    }

    @PostConstruct
    private void init() {
        // Load the map
        tileMap.loadMap("level1.csv");

        // Register input listener

        inputController.registerListener(this::handleInput);
        audioManager.initialize();
        audioManager.loadBackgroundMusic("loop56.wav");

    }

    public void update() {
        // Update player
        player.update();

    }

    public void render() {
        GL11.glPushMatrix();

        // Center the view on the player
        GL11.glTranslatef(screenWidth/2 - player.getX(), screenHeight/2 - player.getY(), 0);

        // Render the tile map with culling
        tileMap.render(player.getX(), player.getY(), screenWidth, screenHeight);

        // Render player with its collision box
        player.render();

        GL11.glPopMatrix();
    }

    public void handleInput(int key, int action) {
        // State change
        if (key == GLFW.GLFW_KEY_V && action == GLFW.GLFW_PRESS) {
            gameStateManager.switchState(GameState.VISUAL_NOVEL);
        }

        // Toggle collision visualization with C key
        if (key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) {
            tileMap.toggleCollisionView();
            player.toggleCollisionView();
        }

        // Movement controls
        if (key == GLFW.GLFW_KEY_SPACE && action == GLFW.GLFW_PRESS) {
            player.jump();
        }

        if (key == GLFW.GLFW_KEY_A) {
            player.setMovingLeft(action == GLFW.GLFW_PRESS);
        }
        if (key == GLFW.GLFW_KEY_D) {
            player.setMovingRight(action == GLFW.GLFW_PRESS);
        }
    }
}