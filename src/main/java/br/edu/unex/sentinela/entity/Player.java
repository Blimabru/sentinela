package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.Tile;
import javafx.scene.input.KeyCode;

/**
 * Representa o protagonista ou a entidade controlada diretamente pelas ações do usuário.
 * Esta classe armazena exclusivamente os dados lógicos (como coordenadas matemáticas) 
 * e as regras que definem como esses dados devem mudar ao longo do tempo.
 */
public class Player {
    
    // Armazenam o estado posicional da entidade dentro do mundo matemático livre (ponto flutuante).
    // "x" e "y" representam o canto superior esquerdo da entidade na tela visual.
    private double x;
    private double y;
    
    // Dimensões espaciais do corpo sólido invisível do jogador (Bounding Box). Utilizado no cálculo das colisões.
    private final double width = 32.0;
    private final double height = 32.0;
    
    // Constante que determina a velocidade física inata de deslocamento em campo aberto.
    private final double baseSpeed = 200.0; 
    
    /**
     * Construtor executado no momento da criação da entidade. Permite que o jogador
     * surja inicialmente em um local fixo predeterminado livre de colisão de forma programática.
     * 
     * @param startX A posição matemática livre no eixo horizontal da tela.
     * @param startY A posição matemática livre no eixo vertical da tela.
     */
    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    /**
     * Realiza a atualização cíclica contínua. Verifica colisões teóricas (matrizes) antes
     * de materializar as coordenadas corporais na tela para não permitir buracos nas leis da física,
     * incluindo penalidades do terreno no custo global de deslocamento.
     * 
     * @param deltaTime Fração de segundo vitalícia da máquina usada como fator de suavização.
     * @param input Componente sensorial contendo o mapa dos reflexos elétricos do teclado.
     * @param tileMap Referência espacial geométrica com os arranjos sólidos e propriedades texturadas.
     */
    public void update(double deltaTime, InputManager input, TileMap tileMap) {
        // Cópia simulada da coordenada real para realização de tentativas matemáticas sem destruir a origem oficial.
        double nextX = this.x;
        double nextY = this.y;

        // Descobre em qual bloco primário do chão (Tile) os "pés" da entidade encostam no centro de seu corpo gravitacional
        int currentTileCol = (int) ((this.x + width / 2) / TileMap.TILE_SIZE);
        int currentTileRow = (int) ((this.y + height / 2) / TileMap.TILE_SIZE);
        Tile currentTile = tileMap.getTile(currentTileCol, currentTileRow);

        // Se por ventura o bloco possuir resistência pesada (Lama), o avanço cronológico sofre severas restrições redutivas.
        double actualSpeed = baseSpeed / currentTile.getMovementCost();

        // Projeção teórica do eixo vertical
        if (input.isKeyPressed(KeyCode.W) || input.isKeyPressed(KeyCode.UP)) {
            nextY -= actualSpeed * deltaTime;
        }
        if (input.isKeyPressed(KeyCode.S) || input.isKeyPressed(KeyCode.DOWN)) {
            nextY += actualSpeed * deltaTime;
        }

        // Aplicação do teorema espacial restritivo AABB (Axis-Aligned Bounding Box) com a topografia lógica do mapa
        // Somente validamos a coordenada Y para garantir o escorregamento pelos obstáculos laterais do mapa
        if (!checkCollision(this.x, nextY, tileMap)) {
            this.y = nextY;
        }

        // Projeção teórica matemática independente do eixo horizontal
        if (input.isKeyPressed(KeyCode.A) || input.isKeyPressed(KeyCode.LEFT)) {
            nextX -= actualSpeed * deltaTime;
        }
        if (input.isKeyPressed(KeyCode.D) || input.isKeyPressed(KeyCode.RIGHT)) {
            nextX += actualSpeed * deltaTime;
        }

        // Nova checagem retangular exata sobre a teoria hipotética do eixo X vis-à-vis toda área circundante adjacente do tabuleiro.
        if (!checkCollision(nextX, this.y, tileMap)) {
            this.x = nextX;
        }
    }

    /**
     * Um radar interno sofisticado que inspeciona todos os cantos do corpo matemático retangular (Bounding Box) do 
     * protagonista e os compara com a Matriz dimensional indexada abstrata para atestar compatibilidade de matéria física.
     * 
     * @param testX Nova coordenada hipotética lateral na cena global.
     * @param testY Nova coordenada hipotética vertical na cena global.
     * @param tileMap Referência espacial geométrica.
     * @return Retorna obrigatoriamente verdadeiro se a transposição do movimento colidirá de frente com matéria impenetrável.
     */
    private boolean checkCollision(double testX, double testY, TileMap tileMap) {
        // Encontra o identificador indexado (linha e coluna numéricos no tabuleiro abstrato) baseado nos cantos extremos
        // imaginários do volume geométrico e sólido do organismo principal: Superior, Inferior, Esquerdo e Direito.
        
        // Canto Esquerdo
        int leftTile = (int) (testX / TileMap.TILE_SIZE);
        // Canto Direito
        int rightTile = (int) ((testX + width - 0.01) / TileMap.TILE_SIZE);
        // Canto Superior
        int topTile = (int) (testY / TileMap.TILE_SIZE);
        // Canto Inferior
        int bottomTile = (int) ((testY + height - 0.01) / TileMap.TILE_SIZE);

        // Subtrai-se minimamente 0.01 para evitar equívocos lógicos da máquina no caso do indivíduo encostar no milímetro
        // limítrofe exato que delimita a fronteira invisível do grid sucessor livre ou impedido.

        // Em posse dos 4 cantos formadores, basta perguntar ao mapa mestre se todas as pontas caem sobre áreas declaradas Walkables.
        if (!tileMap.getTile(leftTile, topTile).isWalkable()) return true;
        if (!tileMap.getTile(rightTile, topTile).isWalkable()) return true;
        if (!tileMap.getTile(leftTile, bottomTile).isWalkable()) return true;
        if (!tileMap.getTile(rightTile, bottomTile).isWalkable()) return true;

        // Ao sobreviver impune de toda averiguação algorítmica espacial, aprova-se matematicamente a livre passagem irrestrita desta massa vetorial.
        return false;
    }

    /**
     * Método de acesso para leitura de coordenadas.
     * @return O valor numérico atual da posição horizontal (X).
     */
    public double getX() {
        return x;
    }

    /**
     * Método de acesso para leitura de coordenadas.
     * @return O valor numérico atual da posição vertical (Y).
     */
    public double getY() {
        return y;
    }

    /**
     * Método analítico para extrair tamanho lateral do objeto corporal maciço de área do ator para o ilustrador final.
     * @return Largura sólida física contínua impenetrável em unidades microscópicas flutuantes globais (pixels estáticos base) 
     */
    public double getWidth() {
        return width;
    }

    /**
     * Método analítico para extrair espessura em profundidade vertical do molde base invisível tangível corpóreo deste figurante principal vivo central
     * @return Altura contínua imperfurável em unidades microscópicas flutuantes fixas elementares globais projetadas ao longo 
     */
    public double getHeight() {
        return height;
    }
}
