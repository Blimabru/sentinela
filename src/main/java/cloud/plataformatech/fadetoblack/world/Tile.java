package cloud.plataformatech.fadetoblack.world;

/**
 * Representa uma célula atômica na grade bidimensional do {@link TileMap}.
 *
 * <p><b>Arquitetura em Duas Camadas ({@code baseType} vs. {@code dynamicOverride}):</b>
 * Armazena separadamente o solo geológico permanente ({@code baseType}, ex.: água ou piso de pedra)
 * e uma eventual sobreposição mecânica temporária ({@code dynamicOverride}, ex.: prancha de ponte
 * ou grade fechada). Essa separação evita a necessidade de manter cópias de backup da matriz e
 * garante restauração exata do terreno original ao alternar obstáculos dinâmicos.</p>
 */
public class Tile {

    // Posição discreta imutável desta célula na grade (coluna = X, linha = Y)
    private final int col;
    private final int row;
    // Terreno geográfico permanente definido durante a construção do bioma
    private TerrainType baseType;
    // Sobreposição transitória aplicada por DynamicObstacle (nula quando inativa)
    private TerrainType dynamicOverride;

    /**
     * Instancia uma célula posicionada na grade com um tipo de terreno fortemente tipado.
     *
     * @param col      Coluna na matriz do mapa.
     * @param row      Linha na matriz do mapa.
     * @param baseType Material permanente inicial desta célula.
     */
    public Tile(int col, int row, TerrainType baseType) {
        this.col = col;
        this.row = row;
        this.baseType = baseType;
        this.dynamicOverride = null;
    }

    /**
     * Construtor legado baseado em códigos numéricos para compatibilidade com protótipos iniciais
     * ou suítes de teste simplificadas (0 = Pedra, 1 = Parede, 2 = Lama).
     *
     * @param type         Código inteiro legado de terreno.
     * @param walkable     Sinaliza se o tile deve permitir passagem.
     * @param movementCost Peso nominal legado (preservado na assinatura por compatibilidade).
     */
    public Tile(int type, boolean walkable, int movementCost) {
        this.col = 0;
        this.row = 0;
        if (type == 1 || !walkable) {
            this.baseType = TerrainType.WALL;
        } else if (type == 2) {
            this.baseType = TerrainType.MUD;
        } else {
            this.baseType = TerrainType.STONE_FLOOR;
        }
    }

    /**
     * Resolve o material efetivo ativo neste instante, priorizando a sobreposição dinâmica caso exista.
     *
     * @return {@code dynamicOverride} quando presente; caso contrário, {@code baseType}.
     */
    public TerrainType getEffectiveType() {
        return dynamicOverride != null ? dynamicOverride : baseType;
    }

    /**
     * Informa se entidades e rotas A* podem atravessar esta célula no estado atual.
     *
     * @return {@code true} se o terreno efetivo for transitável.
     */
    public boolean isWalkable() {
        return getEffectiveType().isWalkable();
    }

    /**
     * Retorna o custo nominal de travessia do terreno efetivo para consultas do algoritmo A*.
     *
     * @return Peso heurístico do solo ativo.
     */
    public int getMovementCost() {
        return getEffectiveType().getMovementCost();
    }

    /**
     * Retorna o multiplicador físico de velocidade imposto pelo terreno efetivo atual.
     *
     * @return Fator multiplicativo de velocidade.
     */
    public double getSpeedMultiplier() {
        return getEffectiveType().getSpeedMultiplier();
    }

    /**
     * Converte o {@link TerrainType} efetivo em código inteiro legado para interoperabilidade com
     * consumidores antigos de matrizes numéricas.
     *
     * @return Código numérico entre 0 (neutro) e 6 (portal), ou 1 para qualquer bloqueio sólido.
     */
    public int getType() {
        TerrainType current = getEffectiveType();
        if (!current.isWalkable()) return 1;
        if (current == TerrainType.MUD) return 2;
        if (current == TerrainType.TAR) return 3;
        if (current == TerrainType.ICE) return 4;
        if (current == TerrainType.DEEP_SNOW) return 5;
        if (current == TerrainType.PORTAL) return 6;
        return 0;
    }

    /**
     * Retorna o índice de coluna desta célula na matriz.
     *
     * @return Coluna na grade.
     */
    public int getCol() {
        return col;
    }

    /**
     * Retorna o índice de linha desta célula na matriz.
     *
     * @return Linha na grade.
     */
    public int getRow() {
        return row;
    }

    /**
     * Retorna o terreno estrutural permanente subjacente, ignorando sobreposições dinâmicas.
     *
     * @return Tipo base da célula.
     */
    public TerrainType getBaseType() {
        return baseType;
    }

    /**
     * Substitui o terreno estrutural permanente desta célula.
     *
     * @param baseType Novo material base.
     */
    public void setBaseType(TerrainType baseType) {
        this.baseType = baseType;
    }

    /**
     * Aplica uma camada transitória sobre o solo base (usado por portões fechados e pontes baixadas).
     *
     * @param dynamicOverride Tipo de terreno temporário que assumirá o comportamento efetivo.
     */
    public void setDynamicOverride(TerrainType dynamicOverride) {
        this.dynamicOverride = dynamicOverride;
    }

    /**
     * Remove qualquer sobreposição dinâmica ativa, restaurando imediatamente o {@code baseType} original.
     */
    public void clearDynamicOverride() {
        this.dynamicOverride = null;
    }

    /**
     * Verifica se há atualmente algum obstáculo dinâmico alterando a natureza desta célula.
     *
     * @return {@code true} se {@code dynamicOverride} não for nulo.
     */
    public boolean hasDynamicOverride() {
        return dynamicOverride != null;
    }
}
