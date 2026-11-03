package cloud.plataformatech.fadetoblack.world;

/**
 * Representa um obstáculo mecânico móvel no cenário (portões de ferro,
 * portas deslizantes e pontes levadiças).
 * Gerencia seu próprio ciclo temporal de abertura/fechamento e altera
 * dinamicamente a malha de transitabilidade do mapa, forçando o algoritmo A*
 * a recalcular rotas em tempo real.
 */
public class DynamicObstacle {

    public enum ObstacleType {
        GATE("Portão de Aço"),
        SLIDING_DOOR("Porta Deslizante"),
        DRAWBRIDGE("Ponte Levadiça");

        private final String label;

        ObstacleType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String id;
    private final ObstacleType type;
    private final int col;
    private final int row;
    private final int widthTiles;
    private final int heightTiles;

    private boolean open;
    private double openDuration;
    private double closedDuration;
    private double stateTimer;
    private double transitionProgress; // 0.0 (fechado) a 1.0 (aberto)
    private boolean stateChangedThisFrame = false;

    public DynamicObstacle(String id, ObstacleType type, int col, int row, int widthTiles, int heightTiles,
                           boolean startsOpen, double openDuration, double closedDuration) {
        this.id = id;
        this.type = type;
        this.col = col;
        this.row = row;
        this.widthTiles = widthTiles;
        this.heightTiles = heightTiles;
        this.open = startsOpen;
        this.openDuration = openDuration;
        this.closedDuration = closedDuration;
        this.stateTimer = startsOpen ? openDuration : closedDuration;
        this.transitionProgress = startsOpen ? 1.0 : 0.0;
    }

    public void update(double deltaTime, TileMap tileMap) {
        stateChangedThisFrame = false;
        stateTimer -= deltaTime;

        // Atualização suave da transição gráfica
        double targetProgress = open ? 1.0 : 0.0;
        double speed = 3.0; // Velocidade da animação mecânica
        if (transitionProgress < targetProgress) {
            transitionProgress = Math.min(targetProgress, transitionProgress + speed * deltaTime);
        } else if (transitionProgress > targetProgress) {
            transitionProgress = Math.max(targetProgress, transitionProgress - speed * deltaTime);
        }

        // Transição cíclica periódica
        if (stateTimer <= 0) {
            toggle(tileMap);
        }
    }

    /**
     * Alterna o estado do obstáculo e atualiza os tiles sobrepostos no mapa.
     */
    public void toggle(TileMap tileMap) {
        this.open = !this.open;
        this.stateTimer = this.open ? openDuration : closedDuration;
        this.stateChangedThisFrame = true;
        applyToMap(tileMap);
    }

    /**
     * Sincroniza a grade física do TileMap com o estado atual deste obstáculo.
     */
    public void applyToMap(TileMap tileMap) {
        for (int c = col; c < col + widthTiles; c++) {
            for (int r = row; r < row + heightTiles; r++) {
                Tile tile = tileMap.getTile(c, r);
                if (tile == null) continue;

                if (type == ObstacleType.DRAWBRIDGE) {
                    // Ponte levadiça: quando aberta (abaixada), fornece passagem de madeira;
                    // quando fechada (erguida), expõe o terreno intransponível base (água ou abismo).
                    if (open) {
                        tile.setDynamicOverride(TerrainType.BRIDGE_PLANK);
                    } else {
                        tile.clearDynamicOverride();
                    }
                } else {
                    // Portões e portas deslizantes: quando fechados, impõem uma barreira sólida;
                    // quando abertos, removem a barreira e liberam o piso original.
                    if (open) {
                        tile.clearDynamicOverride();
                    } else {
                        tile.setDynamicOverride(TerrainType.GATE_BARRIER);
                    }
                }
            }
        }
    }

    public boolean occupies(int testCol, int testRow) {
        return testCol >= col && testCol < col + widthTiles &&
               testRow >= row && testRow < row + heightTiles;
    }

    public String getId() {
        return id;
    }

    public ObstacleType getType() {
        return type;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getWidthTiles() {
        return widthTiles;
    }

    public int getHeightTiles() {
        return heightTiles;
    }

    public boolean isOpen() {
        return open;
    }

    public double getStateTimer() {
        return stateTimer;
    }

    public double getTransitionProgress() {
        return transitionProgress;
    }

    public boolean isStateChangedThisFrame() {
        return stateChangedThisFrame;
    }
}

