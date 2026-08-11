package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.app.GameApplication;

/**
 * Esta classe funciona como a grande "caixa" invisível do universo do jogo. 
 * Seu objetivo é agrupar, instanciar e gerenciar o ciclo de vida lógico de tudo o que compõe o ambiente 
 * simulado, como o jogador, os futuros mapas, inimigos, paredes e itens.
 */
public class GameWorld {
    
    // Armazena uma referência única e persistente para a entidade central controlada pelo usuário.
    private final Player player;

    /**
     * Ao ser instanciado, o mundo inicia um processo de "criação" de seu conteúdo. 
     * Neste momento de fundação, define-se onde cada objeto deverá surgir pela primeira vez.
     */
    public GameWorld() {
        // A matemática da divisão por dois determina perfeitamente o centro do espaço visual disponível.
        // O jogador, portanto, é gerado em coordenadas absolutas que correspondem ao meio exato da tela.
        this.player = new Player(GameApplication.WIDTH / 2.0, GameApplication.HEIGHT / 2.0);
    }

    /**
     * Este é o canal por onde a pulsação matemática vinda do "GameEngine" atinge os objetos simulados.
     * A responsabilidade do "GameWorld" não é realizar as contas, mas sim distribuir o comando de 
     * atualização cronologicamente para cada componente que o integra.
     * 
     * @param deltaTime Fração de tempo exata gasta no quadro anterior, garantindo uma física justa e uniforme.
     * @param input Componente sensorial repassado aos objetos para que possam processar impulsos humanos (teclado).
     */
    public void update(double deltaTime, InputManager input) {
        // Envia o pulso de tempo e os comandos do teclado diretamente para a entidade do jogador reagir.
        player.update(deltaTime, input);
    }

    /**
     * Provê um mecanismo para que outros sistemas (especialmente o "Renderer") 
     * possam acessar e ler os estados atuais dessa entidade.
     * 
     * @return A referência de memória que aponta para o jogador, permitindo visualizar seus dados internos de posição.
     */
    public Player getPlayer() {
        return player;
    }
}
