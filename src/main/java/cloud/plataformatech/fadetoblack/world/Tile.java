package cloud.plataformatech.fadetoblack.world;

/**
 * Representa uma célula atômica da grade bidimensional do mapa.
 * Encapsula as propriedades geográficas do terreno estático e permite
 * sobreposições dinâmicas (como portões fechados ou pontes estendidas).
 */
public class Tile {

    private final int col;
    private final int row;
    private TerrainType baseType;
    private TerrainType dynamicOverride;

    /**
     * Construtor completo com TerrainType tipado.
     */
    public Tile(int col, int row, TerrainType baseType) {
        this.col = col;
        this.row = row;
        this.baseType = baseType;
        this.dynamicOverride = null;
    }

    /**
     * Construtor legado com código numérico para retrocompatibilidade.
     * 0 = Pedra / Grama, 1 = Parede, 2 = Lama.
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

    public TerrainType getEffectiveType() {
        return dynamicOverride != null ? dynamicOverride : baseType;
    }

    public boolean isWalkable() {
        return getEffectiveType().isWalkable();
    }

    public int getMovementCost() {
        return getEffectiveType().getMovementCost();
    }

    public double getSpeedMultiplier() {
        return getEffectiveType().getSpeedMultiplier();
    }

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

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public TerrainType getBaseType() {
        return baseType;
    }

    public void setBaseType(TerrainType baseType) {
        this.baseType = baseType;
    }

    public void setDynamicOverride(TerrainType dynamicOverride) {
        this.dynamicOverride = dynamicOverride;
    }

    public void clearDynamicOverride() {
        this.dynamicOverride = null;
    }

    public boolean hasDynamicOverride() {
        return dynamicOverride != null;
    }
}
