package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.app.GameApplication;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.world.GameWorld;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Renderer {
    private final GraphicsContext gc;

    public Renderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void render(GameWorld world, double deltaTime) {
        // Limpa a tela
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameApplication.WIDTH, GameApplication.HEIGHT);

        // Desenha o jogador
        drawPlayer(world.getPlayer());

        // Desenha informações de debug (FPS e DeltaTime)
        drawDebugInfo(deltaTime);
    }

    private void drawPlayer(Player player) {
        gc.setFill(Color.BLUE);
        // O jogador é um quadrado simples de 32x32
        gc.fillRect(player.getX() - 16, player.getY() - 16, 32, 32);
    }

    private void drawDebugInfo(double deltaTime) {
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 14));
        
        int fps = (int) (1.0 / deltaTime);
        String fpsText = String.format("FPS: %d", fps);
        String dtText = String.format("DeltaTime: %.4f s", deltaTime);

        gc.fillText(fpsText, 10, 20);
        gc.fillText(dtText, 10, 40);
    }
}
