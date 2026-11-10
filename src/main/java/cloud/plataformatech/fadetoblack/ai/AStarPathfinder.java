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
 *
 * <p>Modela o {@link TileMap} como um grafo ponderado em grade ortogonal e calcula a rota de custo
 * mínimo entre origem e alvo. O algoritmo pondera custos variáveis de travessia (ex.: lama = 2,
 * neve profunda = 3, piche = 4) e aceita um modificador estratégico ({@link TerrainCostModifier})
 * para adaptar a avaliação de custos às características de cada arquétipo de IA.</p>
 */
public class AStarPathfinder {

    /**
     * Contrato funcional (padrão Strategy) que permite personalizar dinamicamente o peso de cada
     * {@link Tile} durante a busca sem duplicar a implementação do algoritmo A*.
     */
    public interface TerrainCostModifier {
        /**
         * Calcula o custo percebido por um agente específico ao atravessar a célula informada.
         *
         * @param tile Célula candidata no passo de expansão.
         * @return Peso inteiro positivo aplicado à aresta de entrada nesta célula.
         */
        int modifyCost(Tile tile);
    }

    // Referência ao mapa espacial ativo cujos obstáculos e terrenos definem o grafo de navegação
    private final TileMap tileMap;

    // Métricas analíticas registradas a cada execução para exibição nos painéis de telemetria
    private double lastCalculationTimeMs = 0.0;
    private int nodesExplored = 0;
    private int lastPathCost = 0;
    private int lastInitialHCost = 0;

    /**
     * Inicializa o resolvedor de rotas vinculado a um mapa em grade.
     *
     * @param tileMap Mapa responsável por fornecer dimensões, passabilidade e custos nominais.
     */
    public AStarPathfinder(TileMap tileMap) {
        this.tileMap = tileMap;
    }

    /**
     * Executa a busca A* padrão utilizando os custos nominais de terreno definidos em {@link Tile}.
     *
     * @param startCol  Coluna de origem do agente.
     * @param startRow  Linha de origem do agente.
     * @param targetCol Coluna do objetivo.
     * @param targetRow Linha do objetivo.
     * @return Lista ordenada de nós da origem (excluída) até o alvo; lista vazia se inacessível.
     */
    public List<Node> findPath(int startCol, int startRow, int targetCol, int targetRow) {
        return findPath(startCol, startRow, targetCol, targetRow, tile -> tile.getMovementCost());
    }

    /**
     * Executa a busca heurística informada aplicando um modificador de custo específico por arquétipo.
     *
     * @param startCol     Coluna inicial do agente na grade.
     * @param startRow     Linha inicial do agente na grade.
     * @param targetCol    Coluna de destino (posição do jogador).
     * @param targetRow    Linha de destino (posição do jogador).
     * @param costModifier Estratégia de ponderação de terreno (se {@code null}, usa custo padrão).
     * @return Sequência ordenada de waypoints até o destino, ou lista vazia caso bloqueado/inválido.
     */
    public List<Node> findPath(int startCol, int startRow, int targetCol, int targetRow, TerrainCostModifier costModifier) {
        // Inicia cronômetro monotônico de alta precisão em nanossegundos para medir latência real
        long startTime = System.nanoTime();
        nodesExplored = 0;
        lastPathCost = 0;

        // Guarda de segurança 1: rejeita imediatamente coordenadas fora dos limites da matriz
        if (!isValidCoord(startCol, startRow) || !isValidCoord(targetCol, targetRow)) {
            lastCalculationTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new ArrayList<>();
        }

        // Guarda de segurança 2: evita explorar todo o grafo caso o alvo esteja em célula intransponível
        Tile targetTile = tileMap.getTile(targetCol, targetRow);
        if (!targetTile.isWalkable()) {
            lastCalculationTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new ArrayList<>();
        }

        // openSet (min-heap): mantém a fronteira de expansão ordenada pelo menor custo total F = G + H
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        // closedSet (tabela hash): armazena nós já avaliados em complexidade O(1) médio para evitar ciclos
        Set<Node> closedSet = new HashSet<>();

        Node startNode = new Node(startCol, startRow);
        Node targetNode = new Node(targetCol, targetRow);

        startNode.setGCost(0);
        int initialH = calculateManhattanDistance(startNode, targetNode);
        startNode.setHCost(initialH);
        this.lastInitialHCost = initialH;

        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            // Extrai da fila de prioridade o nó fronteiriço mais promissor (menor F, desempate por H)
            Node currentNode = openSet.poll();
            closedSet.add(currentNode);
            nodesExplored++;

            // Condição de parada: ao retirar o vértice alvo da PriorityQueue, a otimalidade do caminho G está garantida
            if (currentNode.equals(targetNode)) {
                this.lastPathCost = currentNode.getGCost();
                List<Node> path = retracePath(startNode, currentNode);
                this.lastCalculationTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
                return path;
            }

            // Expande os 4 vizinhos ortogonais (Norte, Sul, Oeste, Leste), eliminando corte de quinas em paredes
            for (Node neighbor : getNeighbors(currentNode)) {
                Tile neighborTile = tileMap.getTile(neighbor.getCol(), neighbor.getRow());

                // Ignora células já expandidas (closedSet) ou obstáculos físicos (paredes, rios, portões fechados)
                if (closedSet.contains(neighbor) || !neighborTile.isWalkable()) {
                    continue;
                }

                // Aplica o peso biológico do arquétipo; exige custo mínimo >= 1 para preservar admissibilidade da heurística
                int stepCost = costModifier != null ? costModifier.modifyCost(neighborTile) : neighborTile.getMovementCost();
                if (stepCost < 1) stepCost = 1;

                int tentativeGCost = currentNode.getGCost() + stepCost;

                // Se o vizinho ainda não compõe a fronteira ou se um atalho mais barato foi encontrado, atualiza seus custos
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

        // Fronteira esgotada sem alcançar o alvo: rota inexistente devido a barreiras fechadas/isolamento
        this.lastCalculationTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return new ArrayList<>();
    }

    /**
     * Reconstrói o caminho ótimo retrocedendo pelos ponteiros {@code parent} do destino até a origem.
     *
     * @param startNode Vértice inicial da busca (excluído da rota final pois o agente já está nele).
     * @param endNode   Vértice final alcançado contendo a cadeia completa de antecessores.
     * @return Lista de waypoints ordenada do próximo passo imediato até o alvo.
     */
    private List<Node> retracePath(Node startNode, Node endNode) {
        List<Node> path = new ArrayList<>();
        Node current = endNode;

        while (current != null && !current.equals(startNode)) {
            path.add(current);
            current = current.getParent();
        }

        // Inverte a lista acumulada (destino -> origem) para obter a ordem de deslocamento (origem -> destino)
        Collections.reverse(path);

        // Atribui numeração sequencial (1..N) para visualização dos passos sobre o mapa no Renderer
        for (int i = 0; i < path.size(); i++) {
            path.get(i).setStepIndex(i + 1);
        }

        return path;
    }

    /**
     * Gera os vizinhos cardeais válidos (conectividade 4-way) adjacentes ao vértice informado.
     * Usa 4 direções em vez de 8 para manter coerência estrita com a métrica de Manhattan e impedir
     * que agentes fiquem presos atravessando diagonais coladas a quinas de paredes.
     *
     * @param node Vértice atualmente expandido.
     * @return Lista contendo até 4 nós vizinhos dentro dos limites da grade.
     */
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

    /**
     * Calcula a distância de Manhattan ({@code |Δcol| + |Δrow|}) entre dois nós.
     * Como todo passo custa no mínimo {@code 1}, esta heurística jamais superestima o custo real,
     * satisfazendo o critério de admissibilidade necessário para que o A* encontre o ótimo global.
     *
     * @param a Nó de partida da estimativa.
     * @param b Nó de destino.
     * @return Menor número possível de passos ortogonais até {@code b}.
     */
    private int calculateManhattanDistance(Node a, Node b) {
        return Math.abs(a.getCol() - b.getCol()) + Math.abs(a.getRow() - b.getRow());
    }

    /**
     * Valida se uma coordenada matricial pertence ao intervalo válido da grade ativa.
     *
     * @param col Índice de coluna a testar.
     * @param row Índice de linha a testar.
     * @return {@code true} se estiver contida dentro de {@code [0..cols-1] x [0..rows-1]}.
     */
    private boolean isValidCoord(int col, int row) {
        return col >= 0 && col < tileMap.getCols() && row >= 0 && row < tileMap.getRows();
    }

    /**
     * Retorna o tempo gasto na última execução do A* em milissegundos.
     *
     * @return Latência computacional medida em milissegundos.
     */
    public double getLastCalculationTimeMs() {
        return lastCalculationTimeMs;
    }

    /**
     * Retorna a quantidade total de nós retirados da fila e expandidos na última busca.
     *
     * @return Contagem de vértices explorados no espaço de estados.
     */
    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Retorna o custo real acumulado {@code G} da rota encontrada na última busca.
     *
     * @return Custo total ponderado pelos terrenos ou 0 se nenhuma rota foi encontrada.
     */
    public int getLastPathCost() {
        return lastPathCost;
    }

    /**
     * Retorna a distância heurística inicial {@code H} calculada no ponto de partida da última busca.
     *
     * @return Estimativa inicial de Manhattan até o objetivo.
     */
    public int getLastInitialHCost() {
        return lastInitialHCost;
    }
}
