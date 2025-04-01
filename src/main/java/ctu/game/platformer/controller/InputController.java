package ctu.game.platformer.controller;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InputController implements GLFWKeyCallbackI {

    private final boolean[] keyPressed = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final List<KeyEventListener> listeners = new ArrayList<>();
    private long window = 0;

    private static final int KEY_W = GLFW.GLFW_KEY_W;
    private static final int KEY_A = GLFW.GLFW_KEY_A;
    private static final int KEY_S = GLFW.GLFW_KEY_S;
    private static final int KEY_D = GLFW.GLFW_KEY_D;

    private static final Map<Integer, Integer> KEY_MAPPINGS = new HashMap<>();
    static {
        KEY_MAPPINGS.put(GLFW.GLFW_KEY_DELETE, KEY_D);
    }

    private final List<MouseMoveListener> mouseMoveListeners = new ArrayList<>();
    private final List<MouseClickListener> mouseClickListeners = new ArrayList<>();

    public interface KeyEventListener {
        void onKeyEvent(int key, int action);
    }

    public interface MouseMoveListener {
        void onMouseMove(double xpos, double ypos);
    }

    public interface MouseClickListener {
        void onMouseClick(int button, int action);
    }

    public void registerListener(KeyEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void registerMouseMoveListener(MouseMoveListener listener) {
        mouseMoveListeners.add(listener);
    }

    public void registerMouseClickListener(MouseClickListener listener) {
        mouseClickListeners.add(listener);
    }

    public void initialize(long windowHandle) {
        this.window = windowHandle;

        // Gán callback bàn phím
        GLFW.glfwSetKeyCallback(window, this);

        // Gán callback chuột
        GLFW.glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            for (MouseMoveListener listener : mouseMoveListeners) {
                listener.onMouseMove(xpos, ypos);
            }
        });

        GLFW.glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            for (MouseClickListener listener : mouseClickListeners) {
                listener.onMouseClick(button, action);
            }
        });
    }

    @Override
    public void invoke(long window, int key, int scancode, int action, int mods) {
        if (KEY_MAPPINGS.containsKey(key)) {
            key = KEY_MAPPINGS.get(key);
        }

        if (key >= 0 && key <= GLFW.GLFW_KEY_LAST) {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                keyPressed[key] = true;
            } else if (action == GLFW.GLFW_RELEASE) {
                keyPressed[key] = false;
            }
        }

//        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
//            GLFW.glfwSetWindowShouldClose(window, true);
//            return;
//        }

        notifyKeyEvent(key, action);
    }

    private void notifyKeyEvent(int key, int action) {
        for (KeyEventListener listener : listeners) {
            try {
                listener.onKeyEvent(key, action);
            } catch (Exception e) {
                System.err.println("Error in listener: " + e.getMessage());
            }
        }
    }

    public boolean isKeyPressed(int key) {
        return key >= 0 && key <= GLFW.GLFW_KEY_LAST && keyPressed[key];
    }

    public void update() {
        for (int key : new int[]{KEY_W, KEY_A, KEY_S, KEY_D}) {
            if (isKeyPressed(key)) {
                notifyKeyEvent(key, GLFW.GLFW_PRESS);
            }
        }
    }
    public void unregisterListener(KeyEventListener listener) {
        listeners.remove(listener);
    }

    public void unregisterMouseMoveListener(MouseMoveListener listener) {
        mouseMoveListeners.remove(listener);
    }

    public void unregisterMouseClickListener(MouseClickListener listener) {
        mouseClickListeners.remove(listener);
    }

}
