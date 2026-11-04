package cloud.plataformatech.fadetoblack.world;

import javafx.scene.paint.Color;

/**
 * Entidade de transição dimensional que conecta diferentes mapas temáticos.
 * Quando o jogador pisa no portal, ocorre a transição espacial e o carregamento
 * do novo ambiente e das instâncias de IA correspondentes.
 */
public class Portal {

    private final int col;
    private final int row;
    private final MapType targetMap;
    private final int targetCol;
    private final int targetRow;
    private final String destinationName;
    private final Color portalColor;
    private double animationTimer = 0.0;

    public Portal(int col, int row, MapType targetMap, int targetCol, int targetRow, String destinationName, Color portalColor) {
        this.col = col;
        this.row = row;
        this.targetMap = targetMap;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
        this.destinationName = destinationName;
        this.portalColor = portalColor;
    }

    public void update(double deltaTime) {
        animationTimer += deltaTime * 3.5;
        if (animationTimer > Math.PI * 2) {
            animationTimer -= Math.PI * 2;
        }
    }

    public boolean touches(double entityX, double entityY, double entityWidth, double entityHeight) {
        double portalX = col * TileMap.TILE_SIZE;
        double portalY = row * TileMap.TILE_SIZE;
        double portalSize = TileMap.TILE_SIZE;

        // Tolerância de sobreposição de centro
        double centerX = entityX + entityWidth / 2.0;
        double centerY = entityY + entityHeight / 2.0;

        return centerX >= portalX && centerX < portalX + portalSize &&
               centerY >= portalY && centerY < portalY + portalSize;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public MapType getTargetMap() {
        return targetMap;
    }

    public int getTargetCol() {
        return targetCol;
    }

    public int getTargetRow() {
        return targetRow;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public Color getPortalColor() {
        return portalColor;
    }

    public double getPulse() {
        return (Math.sin(animationTimer) + 1.0) / 2.0; // Normalizado 0.0 a 1.0
    }
}

