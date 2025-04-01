package ctu.game.platformer.service;

import ctu.game.platformer.controller.InputController;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ctu.game.platformer.model.common.GameState;

@Service
public class GameStateManager implements InputController.KeyEventListener {

    private GameState currentState = GameState.HOME;

    private GameState previousState = GameState.HOME;

    @Autowired
    private PlatformerSystem platformerSystem;

    @Autowired
    private HomeSystem homeSystem;

    @Autowired
    private PauseSystem pauseSystem;

    @Autowired
    private VisualNovelSystem visualNovelSystem;

    @Autowired
    private InputController inputController;

    @PostConstruct
    private void init() {
        inputController.registerListener(this);
        pauseSystem.init();
    }
    public void pause() {
        if (currentState != GameState.PAUSE && currentState != GameState.HOME) {
            pauseSystem.pause(currentState);
        }
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
            case PAUSE:
                pauseSystem.update();
                break;
            default:
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
            case PAUSE:
//                // First render the underlying game state
//                if (previousState == GameState.PLATFORM) {
//                    platformerSystem.render();
//                } else if (previousState == GameState.VISUAL_NOVEL) {
//                    visualNovelSystem.render();
//                }
                // Then overlay the pause menu
                pauseSystem.render();
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
        if (currentState == GameState.PAUSE) {
            // When exiting pause state, don't update previousState
            currentState = state;
        } else {
            previousState = currentState;
            currentState = state;
        }
        System.out.println("Switched to state: " + state);
    }

    public GameState getCurrentState() {
        return currentState;
    }

    // Remove the redundant handleInput method - it's causing confusion
    // as it duplicates onKeyEvent functionality
}