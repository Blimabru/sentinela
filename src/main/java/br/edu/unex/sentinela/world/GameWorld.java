package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.app.GameApplication;

public class GameWorld {
    private final Player player;

    public GameWorld() {
        // Inicializa o jogador no centro da tela
        this.player = new Player(GameApplication.WIDTH / 2.0, GameApplication.HEIGHT / 2.0);
    }

    public void update(double deltaTime, InputManager input) {
        player.update(deltaTime, input);
    }

    public Player getPlayer() {
        return player;
    }
}
