package cloud.plataformatech.fadetoblack.world;

/**
 * A classe Tile representa o bloco construtivo mais básico do mundo matemático do jogo.
 * Como cada "ladrilho" em um tabuleiro, o Tile armazena propriedades físicas que dirão
 * ao motor de física se aquele espaço é livre para andar, ou qual o custo de atravessá-lo.
 */
public class Tile {
    
    // Indica se entidades como o jogador e inimigos podem passar por cima deste bloco.
    private final boolean walkable;
    
    // Representa o "peso" ou "esforço" necessário para andar sobre este bloco.
    // Usado em algoritmos de inteligência artificial (como o A*) para decidir rotas.
    private final int movementCost;
    
    // Identificador categórico numérico usado principalmente para definir a aparência.
    // 0 = Chão de Grama (Livre), 1 = Parede de Pedra (Bloqueado), 2 = Lama (Livre, mas com penalidade de velocidade).
    private final int type;

    /**
     * Construtor do bloco do mapa. Ao criar um novo espaço no tabuleiro, devemos 
     * assinalar imediatamente qual a natureza física dele.
     * 
     * @param type Código numérico que define que tipo de terreno é este.
     * @param walkable Define se os atores do jogo podem invadir este espaço.
     * @param movementCost Valor numérico para o custo de movimentação (1 para normal, 2 ou mais para terrenos difíceis).
     */
    public Tile(int type, boolean walkable, int movementCost) {
        this.type = type;
        this.walkable = walkable;
        this.movementCost = movementCost;
    }

    /**
     * @return Retorna a permissão de passagem deste bloco. Verdadeiro se for transitável.
     */
    public boolean isWalkable() {
        return walkable;
    }

    /**
     * @return Retorna a penalidade de travessia estipulada para este terreno.
     */
    public int getMovementCost() {
        return movementCost;
    }

    /**
     * @return Retorna a identificação numérica estrutural do terreno para fins de renderização.
     */
    public int getType() {
        return type;
    }
}
