package br.edu.unex.sentinela.core;

import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.rendering.Renderer;
import br.edu.unex.sentinela.world.GameWorld;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;

public class GameEngine extends AnimationTimer {

    private final InputManager inputManager;
    private final GameWorld gameWorld;
    private final Renderer renderer;
    
    private long lastTime = 0;

    public GameEngine(Scene scene, GraphicsContext gc) {
        this.inputManager = new InputManager(scene);
        this.gameWorld = new GameWorld();
        this.renderer = new Renderer(gc);
    }

    @Override
    public void handle(long now) {
        if (lastTime == 0) {
            lastTime = now;
            return;
        }

        // Calcula o deltaTime em segundos
        double deltaTime = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;

        // Limita o deltaTime para evitar saltos enormes caso a janela trave
        if (deltaTime > 0.1) {
            deltaTime = 0.1;
        }

        update(deltaTime);
        render(deltaTime);
    }

    private void update(double deltaTime) {
        // Atualiza o mundo baseando-se no input
        gameWorld.update(deltaTime, inputManager);
    }

    private void render(double deltaTime) {
        // Desenha o mundo atualizado
        renderer.render(gameWorld, deltaTime);
    }
}
