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

    // fields to track text animation state:
    private int visibleCharCount = 0;
    private long lastCharTime = 0;
    private boolean isTextFullyDisplayed = false;
    private final int CHAR_DELAY_MS = 30;

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
        int dialogBoxHeight = 250;
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
        int dialogBoxHeight = 250;
        int dialogBoxY = screenHeight - dialogBoxHeight - 20;

        // Render character name
        fontRenderer.renderText(dialogue.getCharacterName(), 90, dialogBoxY + 30, 0.7f, 1.0f, 1.0f, 1.0f);

        // Update the text animation
        updateTextAnimation(dialogue.getText());

        // Get the visible portion of the text
        String visibleText = dialogue.getText().substring(0, Math.min(visibleCharCount, dialogue.getText().length()));

        // Render dialogue text
        fontRenderer.renderWrappedText(visibleText, 100, dialogBoxY + 60, screenWidth - 120, 0.6f, 0.9f, 0.9f, 0.9f);

        // Render "click to continue" indicator only when text is fully displayed
        if (isTextFullyDisplayed && getCurrentScene().getDialogues().size() > currentDialogueIndex + 1) {
            fontRenderer.renderText("▼", screenWidth - 80, dialogBoxY + dialogBoxHeight - 30, 0.8f, 0.8f, 0.8f, 0.8f);
        }
    }

    private void updateTextAnimation(String fullText) {
        // Check if text is already fully displayed
        if (visibleCharCount >= fullText.length()) {
            isTextFullyDisplayed = true;
            return;
        }

        // Get current time
        long currentTime = System.currentTimeMillis();

        // Check if enough time has passed to show the next character
        if (currentTime - lastCharTime >= CHAR_DELAY_MS) {
            visibleCharCount++;
            lastCharTime = currentTime;

            // Check if we've reached the end
            if (visibleCharCount >= fullText.length()) {
                isTextFullyDisplayed = true;
            }
        }
    }



    private void renderChoices(List<Choice> choices) {
        int startY = screenHeight / 2 - 220;
        int spacing = 60;
        int width = 400;
        int height = 50;

        for (int i = 0; i < choices.size(); i++) {
            int y = startY + i * spacing;
            Choice choice = choices.get(i);

            // Check if mouse is hovering over this choice
            boolean isHovered = mouseX >= screenWidth/2 - width/2 &&
                    mouseX <= screenWidth/2 + width/2 &&
                    mouseY >= y &&
                    mouseY <= y + height;
            boolean isSelected = i == selectedChoiceIndex;

            // Draw choice box
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, isSelected ? textureIds.get("choice_selected") : textureIds.get("choice_box"));

            // Set color based on hover state
            if (isHovered) {
                GL11.glColor4f(1.0f, 1.0f, 0.8f, 1.0f); // Light yellow tint
            } else {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.9f); // Normal color
            }

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(screenWidth/2 - width/2, y);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(screenWidth/2 + width/2, y);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(screenWidth/2 + width/2, y + height);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(screenWidth/2 - width/2, y + height);
            GL11.glEnd();

            GL11.glDisable(GL11.GL_TEXTURE_2D);

            // Render text with hover effect
            float textBrightness = isHovered ? 1.0f : (isSelected ? 1.0f : 0.8f);
            fontRenderer.renderCenteredText(
                    choice.getText(),
                    screenWidth/2,
                    y + height/2 - 8,
                    0.85f,
                    textBrightness,
                    textBrightness,
                    textBrightness
            );
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
        if (currentScene == null) return;

        List<Dialog> dialogues = currentScene.getDialogues();
        if (dialogues == null || dialogues.isEmpty()) return;

        // Check if current index is valid
        if (currentDialogueIndex >= dialogues.size()) {
            if (isShowingChoices()) {
                // Handle choices
                List<Choice> choices = currentScene.getChoices();
                if (choices == null || choices.isEmpty()) {
                    gameStateManager.switchState(GameState.PLATFORM);
                    return;
                }

                Choice selectedChoice = choices.get(selectedChoiceIndex);
                if (canSelectChoice(selectedChoice)) {
                    String nextScene = selectedChoice.getNextScene();
                    if (nextScene == null || nextScene.isEmpty()) {
                        // Nếu không có scene tiếp theo, chuyển về platform
                        gameStateManager.switchState(GameState.PLATFORM);
                    } else {
                        // Chuyển đến scene tiếp theo
                        currentSceneId = nextScene;
                        currentDialogueIndex = 0;
                        selectedChoiceIndex = 0;
                        resetTextAnimation();
                    }
                }
            } else {
                // Nếu không có choices, chuyển về platform
                gameStateManager.switchState(GameState.PLATFORM);
            }
            return;
        }

        if (!isTextFullyDisplayed) {
            visibleCharCount = dialogues.get(currentDialogueIndex).getText().length();
            isTextFullyDisplayed = true;
            return;
        }

        // Di chuyển đến dialogue tiếp theo
        if (currentDialogueIndex < dialogues.size() - 1) {
            currentDialogueIndex++;
            resetTextAnimation();
        } else {
            if (currentScene.getChoices() != null && !currentScene.getChoices().isEmpty()) {
                currentDialogueIndex = dialogues.size(); // Hiển thị choices
            } else {
                gameStateManager.switchState(GameState.PLATFORM); // Kết thúc scene
            }
        }
    }
    private void resetTextAnimation() {
        visibleCharCount = 0;
        isTextFullyDisplayed = false;
        lastCharTime = System.currentTimeMillis();
    }
    private boolean canSelectChoice(Choice choice) {
        if (choice.getRequiredItem() != null && !inventory.getOrDefault(choice.getRequiredItem(), false)) {
            return false;
        }
        return true;
    }

    private boolean isShowingChoices() {
        Scene currentScene = getCurrentScene();
        if (currentScene == null) return false;

        List<Dialog> dialogues = currentScene.getDialogues();
        if (dialogues == null) return false;

        return currentDialogueIndex >= dialogues.size() &&
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