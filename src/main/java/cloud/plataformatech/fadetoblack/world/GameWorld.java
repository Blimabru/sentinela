package cloud.plataformatech.fadetoblack.world;

import cloud.plataformatech.fadetoblack.entity.Player;
import cloud.plataformatech.fadetoblack.input.InputManager;

/**
 * Esta classe funciona como a grande "caixa" invisível do universo do jogo. 
 * Seu objetivo é agrupar, instanciar e gerenciar o ciclo de vida lógico de tudo o que compõe o ambiente 
 * simulado, como o mapa físico bidimensional e os atores do cenário.
 */
public class GameWorld {
    
    // Armazena uma referência única e persistente para a entidade central controlada pelo usuário.
    private final Player player;
    
    // Armazena a fundação do mundo físico, contendo as propriedades geográficas e obstáculos de cada segmento de terra.
    private final TileMap tileMap;

    /**
     * Ao ser instanciado, o mundo inicia um processo de "criação" de seu conteúdo. 
     * Neste momento de fundação, define-se onde cada objeto deverá surgir pela primeira vez e qual a topologia do mapa.
     */
    public GameWorld() {
        // Inicializa o tabuleiro de jogo (grid numérico) que definirá a área por onde as entidades caminham.
        this.tileMap = new TileMap();
        
        // Posiciona explicitamente o protagonista em uma coordenada válida e predeterminada do tabuleiro lógico
        // convertendo posições do mapa para coordenadas flutuantes livres.
        this.player = new Player(4 * TileMap.TILE_SIZE, 4 * TileMap.TILE_SIZE);
    }

    /**
     * Este é o canal por onde a pulsação matemática vinda do "GameEngine" atinge os objetos simulados.
     * A responsabilidade do "GameWorld" não é realizar as contas, mas sim distribuir o comando de 
     * atualização cronologicamente para cada componente que o integra, injetando as variáveis do ambiente onde necessário.
     * 
     * @param deltaTime Fração de tempo exata gasta no quadro anterior, garantindo uma física justa e uniforme.
     * @param input Componente sensorial repassado aos objetos para que possam processar impulsos humanos (teclado).
     */
    public void update(double deltaTime, InputManager input) {
        // Envia o pulso de tempo, os comandos do teclado e a geometria do mapa atual para a entidade do jogador reagir de forma fisicamente coerente.
        player.update(deltaTime, input, tileMap);
    }

    /**
     * Provê um mecanismo legível para extrair as topografias estáticas programadas internamente no universo.
     * 
     * @return O mapa do jogo construído com blocos.
     */
    public TileMap getTileMap() {
        return tileMap;
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
