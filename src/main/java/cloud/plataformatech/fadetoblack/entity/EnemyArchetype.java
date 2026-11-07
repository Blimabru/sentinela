package cloud.plataformatech.fadetoblack.entity;

import cloud.plataformatech.fadetoblack.world.Tile;
import javafx.scene.paint.Color;

/**
 * Define os arquétipos dos três inimigos autônomos previstos no projeto.
 * Cada arquétipo estabelece velocidade inata, sensibilidade a terrenos,
 * cadência de recálculo no grafo e características visuais.
 */
public enum EnemyArchetype {

    DEMON(
            "Demônio",
            "Bruiser Pesado",
            95.0,
            Color.web("#d90429"),
            Color.web("#ff4d6d"),
            0.35
    ),

    BAT(
            "Morcego",
            "Predador Alado",
            135.0,
            Color.web("#7b2cbf"),
            Color.web("#c77dff"),
            0.28
    ),

    GHOST(
            "Espírito",
            "Espectro Flutuante",
            105.0,
            Color.web("#00b4d8"),
            Color.web("#90e0ef"),
            0.32
    );

    private final String displayName;
    private final String roleDescription;
    private final double baseSpeed;
    private final Color primaryColor;
    private final Color accentColor;
    private final double recalculationDelay;

    EnemyArchetype(String displayName, String roleDescription, double baseSpeed,
                   Color primaryColor, Color accentColor, double recalculationDelay) {
        this.displayName = displayName;
        this.roleDescription = roleDescription;
        this.baseSpeed = baseSpeed;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
        this.recalculationDelay = recalculationDelay;
    }

    /**
     * Aplica as peculiaridades biológicas do inimigo no custo dos nós do A*.
     */
    public int modifyTerrainCost(Tile tile) {
        return switch (this) {
            case DEMON -> tile.getMovementCost();
            case BAT -> tile.getMovementCost() > 1 ? Math.max(1, (tile.getMovementCost() + 1) / 2) : 1;
            case GHOST -> 1; // Fantasma flutua sobre lama, piche e neve sem resistências
        };
    }

    /**
     * Calcula o fator de velocidade real conforme o terreno físico sob os pés.
     */
    public double calculateSpeedFactor(Tile tile) {
        return switch (this) {
            case DEMON -> tile.getSpeedMultiplier();
            case BAT -> tile.getSpeedMultiplier() < 1.0
                    ? Math.min(1.0, tile.getSpeedMultiplier() * 1.5)
                    : tile.getSpeedMultiplier();
            case GHOST -> 1.0; // Imune a atolamento ou lentidão de solo
        };
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public Color getAccentColor() {
        return accentColor;
    }

    public double getRecalculationDelay() {
        return recalculationDelay;
    }
}

