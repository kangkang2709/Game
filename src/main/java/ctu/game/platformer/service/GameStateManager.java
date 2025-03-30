package ctu.game.platformer.service;

import ctu.game.platformer.controller.InputController;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ctu.game.platformer.model.common.GameState;

@Service
public class GameStateManager implements InputController.KeyEventListener {

    private GameState currentState = GameState.HOME;

    @Autowired
    private PlatformerSystem platformerSystem;

    @Autowired
    private HomeSystem homeSystem;

    @Autowired
    private VisualNovelSystem visualNovelSystem;

    @Autowired
    private InputController inputController;

    @PostConstruct
    private void init() {
        inputController.registerListener(this);
    }

    public void update() {
        switch (currentState) {
            case PLATFORM:
                platformerSystem.update();
                break;
            case VISUAL_NOVEL:
                visualNovelSystem.update();
                break;
            case HOME:
                homeSystem.update();
                break;
            default:
                // Handle other states
                break;
        }
    }


    public void render() {
        switch (currentState) {
            case PLATFORM:
                platformerSystem.render();
                break;
            case VISUAL_NOVEL:
                visualNovelSystem.render();
                break;
            case HOME:
                homeSystem.render();
                break;
            default:
                // Handle other states
                break;
        }
    }


    @Override
    public void onKeyEvent(int key, int action) {
        switch (currentState) {
            case PLATFORM:
                platformerSystem.handleInput(key, action);
                break;
            case VISUAL_NOVEL:
                visualNovelSystem.handleInput(key, action);
                break;
            case HOME:
                homeSystem.handleInput(key, action);
                break;
            default:
                // Handle other states
                break;
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