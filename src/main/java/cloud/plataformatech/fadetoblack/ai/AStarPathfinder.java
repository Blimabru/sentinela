package cloud.plataformatech.fadetoblack.ai;

import cloud.plataformatech.fadetoblack.world.Tile;
import cloud.plataformatech.fadetoblack.world.TileMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Motor de busca heurística informada baseada no Algoritmo A* (A-Star).
 * Calcula o trajeto ótimo em um grafo em grade, ponderando custos variáveis
 * de terreno (lama, piche, neve) e adaptando-se às peculiaridades
 * de movimentação de cada arquétipo de inteligência artificial.
 */
public class AStarPathfinder {

    public interface TerrainCostModifier {
        int modifyCost(Tile tile);
    }

    private final TileMap tileMap;

    // Métricas de telemetria coletadas na última execução
    private double lastCalculationTimeMs = 0.0;
    private int nodesExplored = 0;
    private int lastPathCost = 0;
    private int lastInitialHCost = 0;

    public AStarPathfinder(TileMap tileMap) {
        this.tileMap = tileMap;
    }

    /**
     * Executa a busca padrão com custos nominais de terreno.
     */
    public List<Node> findPath(int startCol, int startRow, int targetCol, int targetRow) {
        return findPath(startCol, startRow, targetCol, targetRow, tile -> tile.getMovementCost());
    }

    /**
     * Executa a busca considerando o modificador de custo do arquétipo de IA.
     */
    public List<Node> findPath(int startCol, int startRow, int targetCol, int targetRow, TerrainCostModifier costModifier) {
        long startTime = System.nanoTime();
        nodesExplored = 0;
        lastPathCost = 0;

        // Se a origem ou o destino estiverem fora dos limites ou o destino for intransponível
        if (!isValidCoord(startCol, startRow) || !isValidCoord(targetCol, targetRow)) {
            lastCalculationTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new ArrayList<>();
        }

        // Se o alvo estiver sobre uma parede intransponível, não tenta calcular
        Tile targetTile = tileMap.getTile(targetCol, targetRow);
        if (!targetTile.isWalkable()) {
            lastCalculationTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new ArrayList<>();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();

        Node startNode = new Node(startCol, startRow);
        Node targetNode = new Node(targetCol, targetRow);

        startNode.setGCost(0);
        int initialH = calculateManhattanDistance(startNode, targetNode);
        startNode.setHCost(initialH);
        this.lastInitialHCost = initialH;

        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node currentNode = openSet.poll();
            closedSet.add(currentNode);
            nodesExplored++;

            // Condição de chegada ao alvo
            if (currentNode.equals(targetNode)) {
                this.lastPathCost = currentNode.getGCost();
                List<Node> path = retracePath(startNode, currentNode);
                this.lastCalculationTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
                return path;
            }

            // Avaliação dos 4 vizinhos cardeais (Cima, Baixo, Esquerda, Direita)
            for (Node neighbor : getNeighbors(currentNode)) {
                Tile neighborTile = tileMap.getTile(neighbor.getCol(), neighbor.getRow());

                // Descarta se já processado ou se for intransponível (paredes, portões fechados, pontes erguidas)
                if (closedSet.contains(neighbor) || !neighborTile.isWalkable()) {
                    continue;
                }

                int stepCost = costModifier != null ? costModifier.modifyCost(neighborTile) : neighborTile.getMovementCost();
                if (stepCost < 1) stepCost = 1;

                int tentativeGCost = currentNode.getGCost() + stepCost;

                boolean isNew = !openSet.contains(neighbor);
                if (isNew || tentativeGCost < neighbor.getGCost()) {
                    neighbor.setGCost(tentativeGCost);
                    neighbor.setHCost(calculateManhattanDistance(neighbor, targetNode));
                    neighbor.setParent(currentNode);

                    if (isNew) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // Nenhuma rota encontrada (jogador inacessível ou encurralado)
        this.lastCalculationTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return new ArrayList<>();
    }

    private List<Node> retracePath(Node startNode, Node endNode) {
        List<Node> path = new ArrayList<>();
        Node current = endNode;

        while (current != null && !current.equals(startNode)) {
            path.add(current);
            current = current.getParent();
        }

        Collections.reverse(path);

        // Numera os passos ordinais para renderização de depuração
        for (int i = 0; i < path.size(); i++) {
            path.get(i).setStepIndex(i + 1);
        }

        return path;
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>(4);
        int[][] deltas = {
                {0, -1}, // Norte
                {0, 1},  // Sul
                {-1, 0}, // Oeste
                {1, 0}   // Leste
        };

        for (int[] d : deltas) {
            int nc = node.getCol() + d[0];
            int nr = node.getRow() + d[1];
            if (isValidCoord(nc, nr)) {
                neighbors.add(new Node(nc, nr));
            }
        }
        return neighbors;
    }

    private int calculateManhattanDistance(Node a, Node b) {
        return Math.abs(a.getCol() - b.getCol()) + Math.abs(a.getRow() - b.getRow());
    }

    private boolean isValidCoord(int col, int row) {
        return col >= 0 && col < tileMap.getCols() && row >= 0 && row < tileMap.getRows();
    }

    // Getters de telemetria
    public double getLastCalculationTimeMs() {
        return lastCalculationTimeMs;
    }

    public int getNodesExplored() {
        return nodesExplored;
    }

    public int getLastPathCost() {
        return lastPathCost;
    }

    public int getLastInitialHCost() {
        return lastInitialHCost;
    }
}
