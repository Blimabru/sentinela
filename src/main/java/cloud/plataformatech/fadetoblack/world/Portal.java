package cloud.plataformatech.fadetoblack.world;

import javafx.scene.paint.Color;

/**
 * Entidade de transição dimensional responsável por interligar os três biomas do mundo.
 *
 * <p>Cada portal mapeia uma célula de origem {@code (col, row)} para um mapa e coordenada de
 * chegada específicos, mantendo ainda um acumulador trigonométrico para animar o brilho pulsante
 * do vórtice arcano na camada de renderização.</p>
 */
public class Portal {

    // Posição discreta do portal na grade do mapa atual
    private final int col;
    private final int row;
    // Bioma de destino e coordenadas matriciais onde o protagonista surgirá após atravessar
    private final MapType targetMap;
    private final int targetCol;
    private final int targetRow;
    // Nome amigável do destino e assinatura cromática do vórtice (corresponde ao bioma de saída)
    private final String destinationName;
    private final Color portalColor;
    // Acumulador angular [0, 2π) para animação senoidal contínua
    private double animationTimer = 0.0;

    /**
     * Instancia um portal interdimensional com origem, destino e identidade visual definidos.
     *
     * @param col             Coluna do portal no mapa atual.
     * @param row             Linha do portal no mapa atual.
     * @param targetMap       Bioma carregado ao atravessar este portal.
     * @param targetCol       Coluna de surgimento do jogador no mapa de destino.
     * @param targetRow       Linha de surgimento do jogador no mapa de destino.
     * @param destinationName Rótulo descritivo do bioma de destino.
     * @param portalColor     Cor temática aplicada aos anéis do vórtice.
     */
    public Portal(int col, int row, MapType targetMap, int targetCol, int targetRow, String destinationName, Color portalColor) {
        this.col = col;
        this.row = row;
        this.targetMap = targetMap;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
        this.destinationName = destinationName;
        this.portalColor = portalColor;
    }

    /**
     * Avança a fase trigonométrica da animação de pulso do portal de forma independente de FPS.
     * Normaliza o ângulo módulo {@code 2π} para prevenir crescimento ilimitado em sessões longas.
     *
     * @param deltaTime Intervalo de simulação do quadro em segundos.
     */
    public void update(double deltaTime) {
        animationTimer += deltaTime * 3.5;
        if (animationTimer > Math.PI * 2) {
            animationTimer -= Math.PI * 2;
        }
    }

    /**
     * Avalia se uma entidade entrou efetivamente no núcleo deste portal.
     * Utiliza o ponto central da hitbox da entidade (em vez de mera interseção de bordas) para
     * evitar teletransportes acidentais quando o jogador apenas passa tangenciando o bloco vizinho.
     *
     * @param entityX      Coordenada X superior esquerda da entidade em pixels.
     * @param entityY      Coordenada Y superior esquerda da entidade em pixels.
     * @param entityWidth  Largura da caixa de colisão da entidade em pixels.
     * @param entityHeight Altura da caixa de colisão da entidade em pixels.
     * @return {@code true} se o centro geométrico da entidade estiver dentro do tile do portal.
     */
    public boolean touches(double entityX, double entityY, double entityWidth, double entityHeight) {
        double portalX = col * TileMap.TILE_SIZE;
        double portalY = row * TileMap.TILE_SIZE;
        double portalSize = TileMap.TILE_SIZE;

        double centerX = entityX + entityWidth / 2.0;
        double centerY = entityY + entityHeight / 2.0;

        return centerX >= portalX && centerX < portalX + portalSize &&
               centerY >= portalY && centerY < portalY + portalSize;
    }

    /**
     * Retorna a coluna do portal na matriz do mapa atual.
     *
     * @return Índice de coluna.
     */
    public int getCol() {
        return col;
    }

    /**
     * Retorna a linha do portal na matriz do mapa atual.
     *
     * @return Índice de linha.
     */
    public int getRow() {
        return row;
    }

    /**
     * Retorna o bioma de destino ativado ao cruzar o portal.
     *
     * @return Tipo de mapa alvo.
     */
    public MapType getTargetMap() {
        return targetMap;
    }

    /**
     * Retorna a coluna de aterrissagem do jogador no mapa de destino.
     *
     * @return Índice de coluna de chegada.
     */
    public int getTargetCol() {
        return targetCol;
    }

    /**
     * Retorna a linha de aterrissagem do jogador no mapa de destino.
     *
     * @return Índice de linha de chegada.
     */
    public int getTargetRow() {
        return targetRow;
    }

    /**
     * Retorna o nome descritivo do ambiente de destino.
     *
     * @return Título legível do mapa alvo.
     */
    public String getDestinationName() {
        return destinationName;
    }

    /**
     * Retorna a cor principal dos anéis de energia deste portal.
     *
     * @return Cor JavaFX do portal.
     */
    public Color getPortalColor() {
        return portalColor;
    }

    /**
     * Calcula a intensidade oscilatória instantânea da animação normalizada no intervalo {@code [0.0, 1.0]}.
     *
     * @return Fator senoidal utilizado para modular espessura e raio do vórtice no {@code Renderer}.
     */
    public double getPulse() {
        return (Math.sin(animationTimer) + 1.0) / 2.0;
    }
}

