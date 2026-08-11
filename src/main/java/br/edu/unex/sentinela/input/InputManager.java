package br.edu.unex.sentinela.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

public class InputManager {
    private final Set<KeyCode> activeKeys = new HashSet<>();

    public InputManager(Scene scene) {
        scene.setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));
    }

    public boolean isKeyPressed(KeyCode code) {
        return activeKeys.contains(code);
    }
}
