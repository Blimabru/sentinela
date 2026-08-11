package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.input.InputManager;
import javafx.scene.input.KeyCode;

public class Player {
    private double x;
    private double y;
    private final double speed = 200.0; // pixels per second
    
    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update(double deltaTime, InputManager input) {
        if (input.isKeyPressed(KeyCode.W) || input.isKeyPressed(KeyCode.UP)) {
            y -= speed * deltaTime;
        }
        if (input.isKeyPressed(KeyCode.S) || input.isKeyPressed(KeyCode.DOWN)) {
            y += speed * deltaTime;
        }
        if (input.isKeyPressed(KeyCode.A) || input.isKeyPressed(KeyCode.LEFT)) {
            x -= speed * deltaTime;
        }
        if (input.isKeyPressed(KeyCode.D) || input.isKeyPressed(KeyCode.RIGHT)) {
            x += speed * deltaTime;
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
