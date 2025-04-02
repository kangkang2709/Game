package ctu.game.platformer.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ctu.game.platformer.controller.InputController;
import ctu.game.platformer.model.common.GameState;
import ctu.game.platformer.model.visualnovel.*;
import ctu.game.platformer.util.AudioManager;
import ctu.game.platformer.util.FontRenderer;
import ctu.game.platformer.util.TextureLoader;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisualNovelSystem {
    private final GameStateManager gameStateManager;
    private final InputController inputController;
    private final AudioManager audioManager;
    private final int screenWidth;
    private final int screenHeight;
    private FontRenderer fontRenderer;

    private StoryData storyData;
    private String currentArcId = "chapter_01";
    private String currentSceneId = "scene_01";
    private int currentDialogueIndex = 0;
    private Map<String, Integer> textureIds = new HashMap<>();
    private Map<String, Boolean> inventory = new HashMap<>();
    private int playerSanity = 10;

    private boolean texturesLoaded = false;
    private int selectedChoiceIndex = 0;
    private double mouseX, mouseY;

    @Autowired
    public VisualNovelSystem(
            @Lazy GameStateManager gameStateManager,
            InputController inputController,
            AudioManager audioManager,
            @Autowired int windowWidth,
            @Autowired int windowHeight) {
        this.gameStateManager = gameStateManager;
        this.inputController = inputController;
        this.audioManager = audioManager;
        this.screenWidth = windowWidth;
        this.screenHeight = windowHeight;
    }

    @PostConstruct
    private void init() {
        loadStoryData();
        inputController.registerMouseClickListener(this::onMouseClick);
        inputController.registerMouseMoveListener(this::onMouseMove);
    }

    private void loadStoryData() {
        try {
            ClassPathResource resource = new ClassPathResource("story/arc1.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper mapper = new ObjectMapper();

            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.storyData = mapper.readValue(inputStream, StoryData.class);
            System.out.println("Story data loaded successfully.");

        } catch (IOException e) {
            System.err.println("Failed to load story data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTextures() {
        if (!texturesLoaded) {
            System.out.println("Loading Visual Novel textures");

            if (this.fontRenderer == null) {
                this.fontRenderer = new FontRenderer();
            }
            // Load backgrounds
            textureIds.put("dialog_box", TextureLoader.loadTexture("assets/images/visualnovel/dialog_box.png"));
            textureIds.put("choice_box", TextureLoader.loadTexture("assets/images/visualnovel/choice_box.png"));
            textureIds.put("choice_selected", TextureLoader.loadTexture("assets/images/visualnovel/choice_selected.png"));

            // Default background and character images
            textureIds.put("default_bg", TextureLoader.loadTexture("assets/images/visualnovel/backgrounds/default.png"));

            // Load specific backgrounds for scenes
            Scene currentScene = getCurrentScene();
            if (currentScene != null && currentScene.getBackground() != null) {
                String bgPath = "assets/images/visualnovel/backgrounds/" + currentScene.getBackground();
                textureIds.put(currentScene.getBackground(), TextureLoader.loadTexture(bgPath));
            }

            texturesLoaded = true;
        }
    }

    public void update() {
        if (!texturesLoaded) {
            loadTextures();
        }

        // Play scene music if needed
        Scene currentScene = getCurrentScene();
        if (currentScene != null && currentScene.getMusic() != null) {
            // audioManager.playBackgroundMusic(currentScene.getMusic());
        }
    }

    public void render() {
        if (!texturesLoaded) {
            return;
        }

        Scene currentScene = getCurrentScene();
        if (currentScene == null) return;

        // Render background
        renderBackground(currentScene.getBackground());

        // Render dialogue box
        renderDialogueBox();

        // Render current dialogue
        if (currentScene.getDialogues() != null && currentDialogueIndex < currentScene.getDialogues().size()) {
            Dialog dialog = currentScene.getDialogues().get(currentDialogueIndex);
            renderDialogue(dialog);
        }

        // Render choices if we're at the end of dialogues
        if (currentDialogueIndex >= currentScene.getDialogues().size() && currentScene.getChoices() != null && !currentScene.getChoices().isEmpty()) {
            renderChoices(currentScene.getChoices());
        }
    }

    private void renderBackground(String background) {
        int textureId = textureIds.getOrDefault(background, textureIds.get("default_bg"));

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(screenWidth, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(screenWidth, screenHeight);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(0, screenHeight);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void renderDialogueBox() {
        int dialogBoxHeight = 150;
        int dialogBoxY = screenHeight - dialogBoxHeight - 20;

        // Render the dialogue box
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureIds.get("dialog_box"));
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.9f);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(50, dialogBoxY);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(screenWidth - 50, dialogBoxY);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(screenWidth - 50, dialogBoxY + dialogBoxHeight);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(50, dialogBoxY + dialogBoxHeight);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void renderDialogue(Dialog dialogue) {
        int dialogBoxHeight = 150;
        int dialogBoxY = screenHeight - dialogBoxHeight - 20;

        // Render character name
        fontRenderer.renderText(dialogue.getCharacterName(), 70, dialogBoxY + 30, 18, 1.0f, 1.0f, 1.0f);
        // Render dialogue text
        fontRenderer.renderWrappedText(dialogue.getText(), 70, dialogBoxY + 60, screenWidth - 120, 16, 0.9f, 0.9f, 0.9f);

        // Render "click to continue" indicator
        if (getCurrentScene().getDialogues().size() > currentDialogueIndex + 1) {
            fontRenderer.renderText("▼", screenWidth - 80, dialogBoxY + dialogBoxHeight - 30, 20, 0.8f, 0.8f, 0.8f);
        }
    }

    private void renderChoices(List<Choice> choices) {
        int startY = screenHeight / 2 - 100;
        int spacing = 60;
        int width = 400;
        int height = 50;

        for (int i = 0; i < choices.size(); i++) {
            int y = startY + i * spacing;
            Choice choice = choices.get(i);
            boolean isSelected = i == selectedChoiceIndex;

            // Draw choice box
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, isSelected ? textureIds.get("choice_selected") : textureIds.get("choice_box"));
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.9f);

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(screenWidth/2 - width/2, y);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(screenWidth/2 + width/2, y);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(screenWidth/2 + width/2, y + height);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(screenWidth/2 - width/2, y + height);
            GL11.glEnd();

            GL11.glDisable(GL11.GL_TEXTURE_2D);


            fontRenderer.renderCenteredText(choice.getText(), screenWidth/2, y + height/2 - 8, 16,
                    isSelected ? 1.0f : 0.8f,
                    isSelected ? 1.0f : 0.8f,
                    isSelected ? 1.0f : 0.8f);
        }
    }

    public void handleInput(int key, int action) {
        if (action != GLFW.GLFW_PRESS) return;

        Scene currentScene = getCurrentScene();
        if (currentScene == null) return;

        switch (key) {
            case GLFW.GLFW_KEY_SPACE:
            case GLFW.GLFW_KEY_ENTER:
                handleAdvanceInput();
                break;

            case GLFW.GLFW_KEY_UP:
                if (isShowingChoices()) {
                    selectedChoiceIndex = Math.max(0, selectedChoiceIndex - 1);
                }
                break;

            case GLFW.GLFW_KEY_DOWN:
                if (isShowingChoices()) {
                    selectedChoiceIndex = Math.min(currentScene.getChoices().size() - 1, selectedChoiceIndex + 1);
                }
                break;

            case GLFW.GLFW_KEY_P:
                gameStateManager.switchState(GameState.PLATFORM);
                break;

            case GLFW.GLFW_KEY_ESCAPE:
                gameStateManager.pause();
                break;
        }
    }

    private void handleAdvanceInput() {
        Scene currentScene = getCurrentScene();

        if (currentDialogueIndex < currentScene.getDialogues().size()) {
            // Advance to next dialogue
            currentDialogueIndex++;
        } else if (isShowingChoices()) {
            // Select current choice
            Choice selectedChoice = currentScene.getChoices().get(selectedChoiceIndex);
            if (canSelectChoice(selectedChoice)) {
                currentSceneId = selectedChoice.getNextScene();
                currentDialogueIndex = 0;
                selectedChoiceIndex = 0;

                // Load textures for the new scene
                String bgPath = "vn/backgrounds/" + getCurrentScene().getBackground();
                textureIds.put(getCurrentScene().getBackground(), TextureLoader.loadTexture(bgPath));
            }
        } else if (currentScene.getChoices() == null || currentScene.getChoices().isEmpty()) {
            // End of scene with no choices, return to platformer
            gameStateManager.switchState(GameState.PLATFORM);
        }
    }

    private boolean canSelectChoice(Choice choice) {
        if (choice.getRequiredItem() != null && !inventory.getOrDefault(choice.getRequiredItem(), false)) {
            return false;
        }
        return true;
    }

    private boolean isShowingChoices() {
        Scene currentScene = getCurrentScene();
        return currentDialogueIndex >= currentScene.getDialogues().size() &&
                currentScene.getChoices() != null &&
                !currentScene.getChoices().isEmpty();
    }

    public void onMouseClick(int button, int action) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            if (isShowingChoices()) {
                // Check if a choice was clicked
                Scene currentScene = getCurrentScene();
                int startY = screenHeight / 2 - 100;
                int spacing = 60;
                int width = 400;
                int height = 50;

                for (int i = 0; i < currentScene.getChoices().size(); i++) {
                    int y = startY + i * spacing;

                    if (mouseX >= screenWidth/2 - width/2 && mouseX <= screenWidth/2 + width/2 &&
                            mouseY >= y && mouseY <= y + height) {
                        selectedChoiceIndex = i;
                        handleAdvanceInput();
                        break;
                    }
                }
            } else {
                // Clicking anywhere else advances the story
                handleAdvanceInput();
            }
        }
    }

    public void onMouseMove(double xpos, double ypos) {
        this.mouseX = xpos;
        this.mouseY = ypos;
    }

    private Scene getCurrentScene() {
        if (storyData == null || storyData.getArcs() == null) return null;

        for (Arc arc : storyData.getArcs()) {
            if (arc.getId().equals(currentArcId)) {
                for (Scene scene : arc.getScenes()) {
                    if (scene.getId().equals(currentSceneId)) {
                        return scene;
                    }
                }
            }
        }
        return null;
    }

    @PreDestroy
    public void cleanup() {
        releaseResources();
    }

    public void releaseResources() {
        if (texturesLoaded) {
            System.out.println("Releasing VisualNovelSystem resources");
            for (Integer textureId : textureIds.values()) {
//                GL11.glDeleteTextures(textureId);
            }
            textureIds.clear();
            texturesLoaded = false;
        }
    }
}