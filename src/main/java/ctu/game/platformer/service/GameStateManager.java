package ctu.game.platformer.service;

import ctu.game.platformer.controller.InputController;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ctu.game.platformer.model.common.GameState;

@Service
public class GameStateManager implements InputController.KeyEventListener {

    private GameState currentState = GameState.PLATFORM;

    @Autowired
    private PlatformerSystem platformerSystem;

    @Autowired
    private VisualNovelSystem visualNovelSystem;

    @Autowired
    private InputController inputController;

    @PostConstruct
    private void init() {
        inputController.registerListener(this);
    }

    public void update() {
        if (currentState == GameState.PLATFORM) {
            platformerSystem.update();
        } else {
            visualNovelSystem.update();
        }
    }

    public void render() {
        if (currentState == GameState.PLATFORM) {
            platformerSystem.render();
        } else {
            visualNovelSystem.render();
        }
    }

    @Override
    public void onKeyEvent(int key, int action) {
        if (currentState == GameState.PLATFORM) {
            platformerSystem.handleInput(key, action);
        } else {
            visualNovelSystem.handleInput(key, action);
        }
    }

    public void switchState(GameState state) {
        this.currentState = state;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    // Remove the redundant handleInput method - it's causing confusion
    // as it duplicates onKeyEvent functionality
}