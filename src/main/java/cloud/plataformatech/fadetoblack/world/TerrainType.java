package cloud.plataformatech.fadetoblack.world;

import javafx.scene.paint.Color;

/**
 * Enumeração que define a tipologia de terrenos do jogo, integrando atributos
 * físicos (passabilidade e velocidade) e pesos heurísticos para o algoritmo A*.
 */
public enum TerrainType {

    // Terrenos Padrão / Neutros
    STONE_FLOOR("Pedra Regular", true, 1, 1.0, Color.web("#2b2d42"), Color.web("#3d405b")),
    GRASS("Grama da Floresta", true, 1, 1.0, Color.web("#2d6a4f"), Color.web("#40916c")),
    SNOW_FLOOR("Neve Compacta", true, 1, 1.0, Color.web("#e0e1dd"), Color.web("#ffffff")),

    // Terrenos com Penalidade de Movimento (Dificuldade de Deslocamento)
    MUD("Lamaçal Denso", true, 2, 0.50, Color.web("#582f0e"), Color.web("#7f4f24")),
    TAR("Piche Viscoso", true, 4, 0.25, Color.web("#141115"), Color.web("#262329")),
    DEEP_SNOW("Neve Profunda", true, 3, 0.35, Color.web("#b8c0c8"), Color.web("#d0d6dc")),
    ICE("Gelo Escorregadio", true, 1, 1.35, Color.web("#a8dadc"), Color.web("#f1faee")),

    // Obstáculos Naturais Intransponíveis
    WALL("Muralha de Pedra", false, 0, 0.0, Color.web("#1b1b1e"), Color.web("#373737")),
    WATER("Água Profunda", false, 0, 0.0, Color.web("#1d3557"), Color.web("#457b9d")),
    ABYSS("Abismo Sombrio", false, 0, 0.0, Color.web("#0d0d0d"), Color.web("#1a1a1a")),

    // Estruturas Dinâmicas e Especiais
    PORTAL("Portal Dimensional", true, 1, 1.0, Color.web("#7209b7"), Color.web("#f72585")),
    BRIDGE_PLANK("Madeira da Ponte", true, 1, 1.0, Color.web("#8c5a3c"), Color.web("#a36b47")),
    GATE_BARRIER("Grade de Aço", false, 0, 0.0, Color.web("#4a4e69"), Color.web("#6c757d"));

    private final String displayName;
    private final boolean walkable;
    private final int movementCost;
    private final double speedMultiplier;
    private final Color primaryColor;
    private final Color accentColor;

    TerrainType(String displayName, boolean walkable, int movementCost, double speedMultiplier, Color primaryColor, Color accentColor) {
        this.displayName = displayName;
        this.walkable = walkable;
        this.movementCost = movementCost;
        this.speedMultiplier = speedMultiplier;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isWalkable() {
        return walkable;
    }

    public int getMovementCost() {
        return movementCost;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public Color getAccentColor() {
        return accentColor;
    }
}

