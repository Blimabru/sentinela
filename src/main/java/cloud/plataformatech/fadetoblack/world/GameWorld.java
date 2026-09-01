package cloud.plataformatech.fadetoblack.world;

import cloud.plataformatech.fadetoblack.entity.Player;
import cloud.plataformatech.fadetoblack.entity.Enemy;
import cloud.plataformatech.fadetoblack.input.InputManager;

/**
 * Esta classe funciona como a grande "caixa" invisível do universo do jogo. 
 * Seu objetivo é agrupar, instanciar e gerenciar o ciclo de vida lógico de tudo o que compõe o ambiente 
 * simulado, como o mapa físico bidimensional e os atores do cenário.
 */
public class GameWorld {
    
    // Armazena uma referência única e persistente para a entidade central controlada pelo usuário.
    private final Player player;
    
    // Armazena a referência para a Inteligência Artificial autônoma perseguida.
    private final Enemy enemy;
    
    // Armazena a fundação do mundo físico, contendo as propriedades geográficas e obstáculos de cada segmento de terra.
    private final TileMap tileMap;

    /**
     * Ao ser instanciado, o mundo inicia um processo de "criação" de seu conteúdo. 
     * Neste momento de fundação, define-se onde cada objeto deverá surgir pela primeira vez e qual a topologia do mapa.
     */
    public GameWorld() {
        // Inicializa o tabuleiro de jogo (grid numérico) que definirá a área por onde as entidades caminham.
        this.tileMap = new TileMap();
        
        // Posiciona explicitamente o protagonista em uma coordenada válida e predeterminada do tabuleiro lógico.
        this.player = new Player(4 * TileMap.TILE_SIZE, 4 * TileMap.TILE_SIZE);
        
        // Coloca o vilão no extremo oposto do cenário, forçando-o a atravessar quase todo o mapa (e a lama) para alcançar o jogador.
        this.enemy = new Enemy(17 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, this.tileMap);
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
        // Envia o pulso de tempo, os comandos do teclado e a geometria do mapa atual para a entidade do jogador reagir.
        player.update(deltaTime, input, tileMap);
        
        // Atualiza a mente autônoma do vilão, injetando na percepção dele a posição exata (como coordenada alvo) onde está a sua presa.
        enemy.update(deltaTime, player.getX(), player.getY());
    }

    /**
     * Provê um mecanismo legível para extrair as topografias estáticas programadas internamente no universo.
     */
    public TileMap getTileMap() {
        return tileMap;
    }

    /**
     * Provê um mecanismo para acessar a entidade principal.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Provê um mecanismo para acessar o agente guiado por IA.
     */
    public Enemy getEnemy() {
        return enemy;
    }
}
