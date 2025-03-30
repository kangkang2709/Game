// src/main/java/ctu/game/flatformer/model/visualnovel/Dialog.java
package ctu.game.platformer.model.visualnovel;

public class Dialog {
    private String text;
    private String characterName;
    private String characterImage;

    public Dialog(String text, String characterName, String characterImage) {
        this.text = text;
        this.characterName = characterName;
        this.characterImage = characterImage;
    }

    public String getText() {
        return text;
    }

    public String getCharacterName() {
        return characterName;
    }

    public String getCharacterImage() {
        return characterImage;
    }
}