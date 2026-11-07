package cloud.plataformatech.fadetoblack.entity;

import cloud.plataformatech.fadetoblack.ai.AStarPathfinder;
import cloud.plataformatech.fadetoblack.ai.Node;
import cloud.plataformatech.fadetoblack.world.Tile;
import cloud.plataformatech.fadetoblack.world.TileMap;

import java.util.List;

/**
 * Entidade de Inteligência Artificial autônoma.
 * Cada inimigo utiliza sua própria instância do algoritmo A* parametrizada
 * com base em seu arquétipo (Demônio, Morcego ou Espírito).
 * Mantém telemetria em tempo real das métricas do algoritmo para exibição no HUD overhead.
 */
public class Enemy {

    private final EnemyArchetype archetype;
    private double x;
    private double y;
    private final double width = 32.0;
    private final double height = 32.0;

    private final TileMap tileMap;
    private final AStarPathfinder pathfinder;
    private List<Node> currentPath;

    private double pathCalculationTimer = 0.0;
    private double animationTimer = 0.0;

    // --- Métricas de Telemetria e Diagnóstico da IA ---
    private String aiState = "INICIANDO";
    private double currentSpeed = 0.0;
    private double currentSlowPercent = 0.0;
    private String currentTerrainName = "Pedra";
    private int currentTerrainCost = 1;
    private double astarLatencyMs = 0.0;
    private int nodesExplored = 0;
    private int pathCost = 0;
    private int heuristicDistance = 0;

    public Enemy(double startX, double startY, TileMap tileMap) {
        this(EnemyArchetype.DEMON, startX, startY, tileMap);
    }

    public Enemy(EnemyArchetype archetype, double startX, double startY, TileMap tileMap) {
        this.archetype = archetype;
        this.x = startX;
        this.y = startY;
        this.tileMap = tileMap;
        this.pathfinder = new AStarPathfinder(tileMap);
        this.currentSpeed = archetype.getBaseSpeed();
        this.pathCalculationTimer = archetype.getRecalculationDelay(); // Dispara cálculo inicial imediatamente
    }

    /**
     * Atualiza o ciclo vital da IA: avalia ambiente, executa busca heurística e desloca o agente.
     */
    public void update(double deltaTime, double targetX, double targetY) {
        animationTimer += deltaTime * 5.0;
        pathCalculationTimer += deltaTime;

        // Identifica o tile atual sob os pés do agente
        int currentTileCol = (int) ((this.x + width / 2.0) / TileMap.TILE_SIZE);
        int currentTileRow = (int) ((this.y + height / 2.0) / TileMap.TILE_SIZE);
        Tile currentTile = tileMap.getTile(currentTileCol, currentTileRow);

        // Fator de velocidade ajustado pelo terreno e biologia do arquétipo
        double terrainSpeedFactor = archetype.calculateSpeedFactor(currentTile);
        this.currentTerrainName = currentTile.getEffectiveType().getDisplayName();
        this.currentTerrainCost = archetype.modifyTerrainCost(currentTile);
        this.currentSlowPercent = (1.0 - terrainSpeedFactor) * 100.0;

        // Comportamento comportamental de arquétipo
        double effectiveBaseSpeed = archetype.getBaseSpeed();
        double distToPlayer = Math.hypot((targetX - this.x), (targetY - this.y));

        if (archetype == EnemyArchetype.DEMON) {
            // Demônio entra em fúria ao se aproximar (< 160 px), acelerando +30%
            if (distToPlayer < 160.0) {
                effectiveBaseSpeed *= 1.30;
                this.aiState = "FÚRIA MELEE";
            } else {
                this.aiState = "PERSEGUINDO";
            }
        } else if (archetype == EnemyArchetype.BAT) {
            this.aiState = "ECO-CAÇADA";
        } else if (archetype == EnemyArchetype.GHOST) {
            this.aiState = "FLUTUANDO";
        }

        this.currentSpeed = effectiveBaseSpeed * terrainSpeedFactor;

        // Verifica se a rota atual foi subitamente interrompida por um portão fechado ou ponte erguida
        if (isCurrentPathBlocked()) {
            recalculatePath(targetX, targetY);
        } else if (pathCalculationTimer >= archetype.getRecalculationDelay()) {
            recalculatePath(targetX, targetY);
        }

        // --- Execução Motora do Caminho (Waypoints) ---
        followPath(deltaTime);
    }

    /**
     * Força a execução do algoritmo A* e extrai telemetria de desempenho.
     */
    public void recalculatePath(double targetX, double targetY) {
        pathCalculationTimer = 0.0;

        int startCol = (int) ((this.x + width / 2.0) / TileMap.TILE_SIZE);
        int startRow = (int) ((this.y + height / 2.0) / TileMap.TILE_SIZE);

        int targetCol = (int) ((targetX + width / 2.0) / TileMap.TILE_SIZE);
        int targetRow = (int) ((targetY + height / 2.0) / TileMap.TILE_SIZE);

        // Invoca o A* fornecendo o modificador biológico deste arquétipo
        currentPath = pathfinder.findPath(startCol, startRow, targetCol, targetRow,
                tile -> archetype.modifyTerrainCost(tile));

        // Captura métricas do A* para exibição analítica
        this.astarLatencyMs = pathfinder.getLastCalculationTimeMs();
        this.nodesExplored = pathfinder.getNodesExplored();
        this.pathCost = pathfinder.getLastPathCost();
        this.heuristicDistance = pathfinder.getLastInitialHCost();
    }

    /**
     * Detecta se algum dos próximos nós planejados tornou-se intransponível (ex: porta bateu na frente).
     */
    private boolean isCurrentPathBlocked() {
        if (currentPath == null || currentPath.isEmpty()) return false;
        // Inspeciona os próximos 3 passos da rota
        int lookahead = Math.min(3, currentPath.size());
        for (int i = 0; i < lookahead; i++) {
            Node node = currentPath.get(i);
            Tile t = tileMap.getTile(node.getCol(), node.getRow());
            if (!t.isWalkable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Segue a sequência de nós calculada pelo A*.
     */
    private void followPath(double deltaTime) {
        if (currentPath == null || currentPath.isEmpty()) {
            return;
        }

        Node nextStep = currentPath.get(0);
        double targetPixelX = (nextStep.getCol() * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE - width) / 2.0;
        double targetPixelY = (nextStep.getRow() * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE - height) / 2.0;

        double dx = targetPixelX - this.x;
        double dy = targetPixelY - this.y;
        double distance = Math.hypot(dx, dy);

        if (distance > 3.0) {
            this.x += (dx / distance) * currentSpeed * deltaTime;
            this.y += (dy / distance) * currentSpeed * deltaTime;
        } else {
            // Atingiu o nó: consome o waypoint e avança para o próximo
            currentPath.remove(0);
        }
    }

    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        this.currentPath = null;
        this.pathCalculationTimer = archetype.getRecalculationDelay();
    }

    // --- Getters de Renderização e Métricas ---

    public EnemyArchetype getArchetype() {
        return archetype;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public List<Node> getCurrentPath() {
        return currentPath;
    }

    public String getAiState() {
        return aiState;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public double getCurrentSlowPercent() {
        return currentSlowPercent;
    }

    public String getCurrentTerrainName() {
        return currentTerrainName;
    }

    public int getCurrentTerrainCost() {
        return currentTerrainCost;
    }

    public double getAstarLatencyMs() {
        return astarLatencyMs;
    }

    public int getNodesExplored() {
        return nodesExplored;
    }

    public int getPathCost() {
        return pathCost;
    }

    public int getHeuristicDistance() {
        return heuristicDistance;
    }

    public int getStepsRemaining() {
        return currentPath != null ? currentPath.size() : 0;
    }

    public double getAnimationTimer() {
        return animationTimer;
    }
}
