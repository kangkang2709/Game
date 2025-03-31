package ctu.game.platformer.model.level;

public class TransitionPoint {
    private float x;
    private float y;
    private String targetLevel;

    // Default constructor
    public TransitionPoint() {
    }

    public TransitionPoint(float x, float y, String targetLevel) {
        this.x = x;
        this.y = y;
        this.targetLevel = targetLevel;
    }

    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public String getTargetLevel() { return targetLevel; }
}