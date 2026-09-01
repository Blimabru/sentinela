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
 * Esta é a engrenagem central cognitiva dos agentes inimigos. 
 * O algoritmo A* (A-Star) vasculha as opções de deslocamento do terreno, prevendo a rota mais barata 
 * e desviando metodicamente de obstáculos estruturais como as paredes, considerando ainda as poças de lama.
 */
public class AStarPathfinder {
    
    // O ambiente tático que servirá como tabuleiro de xadrez para os cálculos matemáticos preditivos.
    private final TileMap tileMap;

    /**
     * Instancia a ferramenta de busca de rotas.
     * 
     * @param tileMap Referência persistente para o mapa geográfico do universo.
     */
    public AStarPathfinder(TileMap tileMap) {
        this.tileMap = tileMap;
    }

    /**
     * O núcleo duro do A*. Ele executa o processo de "busca informada".
     * 
     * @param startCol Coluna matrizial onde o agente se encontra atualmente.
     * @param startRow Linha matrizial onde o agente se encontra atualmente.
     * @param targetCol Coluna matrizial onde está o alvo desejado (ex: O Jogador).
     * @param targetRow Linha matrizial onde está o alvo desejado.
     * @return Uma lista sequencial de nós (passos) da origem até o destino, ou uma lista vazia se for impossível chegar lá.
     */
    public List<Node> findPath(int startCol, int startRow, int targetCol, int targetRow) {
        
        // A "Fila Aberta" armazena os nós que conhecemos, mas ainda não avaliamos profundamente.
        // A PriorityQueue usará o método "compareTo" do Node para sempre deixar o nó mais promissor no topo.
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        
        // O "Conjunto Fechado" é o cemitério de nós que já foram minuciosamente analisados e descartados.
        Set<Node> closedSet = new HashSet<>();

        // Cria o ponto de partida na memória do robô.
        Node startNode = new Node(startCol, startRow);
        Node targetNode = new Node(targetCol, targetRow);
        
        openSet.add(startNode);

        // Laço ininterrupto de "pensamento": continua vasculhando até a fila acabar (não há caminho) 
        // ou até o alvo ser alcançado.
        while (!openSet.isEmpty()) {
            
            // 1. Extrai o bloco mais promissor (menor custo F) da fila.
            Node currentNode = openSet.poll();
            closedSet.add(currentNode);

            // 2. Condição de Vitória: O agente bateu a cabeça exatamente na célula do jogador?
            if (currentNode.equals(targetNode)) {
                return retracePath(startNode, currentNode);
            }

            // 3. Descobre os vizinhos imediatos e avalia o custo para ir até eles.
            for (Node neighbor : getNeighbors(currentNode)) {
                
                // Se o vizinho já foi vasculhado no passado, ou se trata-se de uma parede de concreto (walkable=false)
                // nós simplesmente o ignoramos e poupamos processamento inútil.
                Tile neighborTile = tileMap.getTile(neighbor.getCol(), neighbor.getRow());
                if (closedSet.contains(neighbor) || !neighborTile.isWalkable()) {
                    continue;
                }

                // Calcula quanto custaria, saindo da origem, pisar neste vizinho.
                // Aqui, o 'getMovementCost()' extrai o custo adicional (como +2 na lama)
                int newMovementCostToNeighbor = currentNode.getGCost() + neighborTile.getMovementCost();

                // Se encontramos um caminho mais curto para esse vizinho, ou se é a primeira vez que o vemos...
                boolean isNewNode = !openSet.contains(neighbor);
                if (newMovementCostToNeighbor < neighbor.getGCost() || isNewNode) {
                    
                    // Atualiza a matemática do vizinho...
                    neighbor.setGCost(newMovementCostToNeighbor);
                    neighbor.setHCost(calculateManhattanDistance(neighbor, targetNode));
                    
                    // Deixa uma migalha de pão: esse vizinho só foi alcançado graças ao currentNode!
                    neighbor.setParent(currentNode);

                    // Adiciona na fila de processamento futuro, caso seja uma novidade.
                    if (isNewNode) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // Caso o laço termine e a fila seque, significa que o jogador está em um bunker isolado. Retorna rota falha.
        return new ArrayList<>();
    }

    /**
     * Retrocede no tempo lendo as "migalhas de pão" (referências ao Parent) deixadas pelos nós
     * durante o sucesso da busca, invertendo a ordem no final para gerar uma rota viável e caminhável.
     * 
     * @param startNode Ponto de origem.
     * @param endNode O troféu (alvo) alcançado.
     * @return A lista ordenada do caminho.
     */
    private List<Node> retracePath(Node startNode, Node endNode) {
        List<Node> path = new ArrayList<>();
        Node currentNode = endNode;

        while (!currentNode.equals(startNode)) {
            path.add(currentNode);
            currentNode = currentNode.getParent();
        }
        
        // Como o caminho foi construído de trás pra frente, precisamos virá-lo ao contrário
        Collections.reverse(path);
        return path;
    }

    /**
     * Lê a matriz para fornecer até 4 vizinhos diretos (Cima, Baixo, Esquerda, Direita).
     * Neste motor, optou-se por não permitir navegação na diagonal.
     */
    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();

        // Projeções cardeais
        int[][] directions = {
            {0, -1}, // Cima
            {0, 1},  // Baixo
            {-1, 0}, // Esquerda
            {1, 0}   // Direita
        };

        for (int[] dir : directions) {
            int checkCol = node.getCol() + dir[0];
            int checkRow = node.getRow() + dir[1];

            // Avalia estritamente se não estamos ultrapassando as fronteiras invisíveis do abismo computacional.
            if (checkCol >= 0 && checkCol < tileMap.getCols() && checkRow >= 0 && checkRow < tileMap.getRows()) {
                neighbors.add(new Node(checkCol, checkRow));
            }
        }

        return neighbors;
    }

    /**
     * A Heurística de Manhattan (ou Geometria do Táxi). Calcula a distância percorrida somando as arestas
     * do cateto horizontal com o vertical, ignorando hipotenusas, o que reflete a realidade de uma grade sem diagonais.
     */
    private int calculateManhattanDistance(Node nodeA, Node nodeB) {
        int dstCol = Math.abs(nodeA.getCol() - nodeB.getCol());
        int dstRow = Math.abs(nodeA.getRow() - nodeB.getRow());
        return dstCol + dstRow;
    }
}
