package cloud.plataformatech.fadetoblack.ai;

/**
 * Representa um nó (vértice) na grade bidimensional de busca do algoritmo A* (A-Star).
 *
 * <p>Encapsula a coordenada matricial discreta {@code (col, row)}, as componentes da função
 * de avaliação {@code F(n) = G(n) + H(n)} e a referência encadeada ao nó antecessor ({@code parent})
 * utilizada para reconstruir o caminho de custo mínimo em tempo linear após atingir o destino.</p>
 */
public class Node implements Comparable<Node> {

    // Coordenadas matriciais declaradas como imutáveis (final) pois sustentam equals() e hashCode();
    // mutar chaves espaciais dentro de HashSet/PriorityQueue corromperia o endereçamento interno
    private final int col;
    private final int row;

    // G(n): custo real acumulado desde o nó inicial até este vértice, ponderado pelos pesos de terreno
    private int gCost;

    // H(n): estimativa heurística admissível (distância de Manhattan) deste vértice até o alvo
    private int hCost;

    // Índice ordinal (1..N) na rota final reconstruída, exibido nos waypoints de depuração da HUD
    private int stepIndex = 0;

    // Ponteiro genealógico ao predecessor de menor custo conhecido na árvore de busca
    private Node parent;

    /**
     * Instancia um vértice correspondente à célula {@code (col, row)} do {@code TileMap}.
     *
     * @param col Índice da coluna na grade (eixo X lógico).
     * @param row Índice da linha na grade (eixo Y lógico).
     */
    public Node(int col, int row) {
        this.col = col;
        this.row = row;
    }

    /**
     * Calcula a pontuação total estimada do caminho passando por este nó: {@code F = G + H}.
     * O algoritmo A* utiliza este valor na fila de prioridade para expandir primeiro os vértices mais promissores.
     *
     * @return Soma inteira entre o custo real percorrido e a estimativa heurística restante.
     */
    public int getFCost() {
        return gCost + hCost;
    }

    /**
     * Define a ordenação natural na {@link java.util.PriorityQueue} ({@code openSet}) do A*.
     *
     * <p>Em caso de empate em {@code F}, o critério de desempate favorece o menor {@code H}
     * (mais próximo geometricamente do objetivo), reduzindo explorações redundantes em regiões de custo uniforme.</p>
     *
     * @param other Outro nó candidato à expansão com o qual este nó será comparado.
     * @return Valor negativo se este nó tiver maior prioridade; zero em empate total; positivo caso contrário.
     */
    @Override
    public int compareTo(Node other) {
        int compare = Integer.compare(this.getFCost(), other.getFCost());
        if (compare == 0) {
            // Desempate heurístico: prioriza avançar em direção ao alvo em vez de expandir lateralmente
            compare = Integer.compare(this.hCost, other.hCost);
        }
        return compare;
    }

    /**
     * Determina identidade topológica exclusivamente pelas coordenadas {@code (col, row)},
     * ignorando custos ou antecessores temporários. Isso permite que {@code closedSet.contains()}
     * e {@code openSet.contains()} reconheçam imediatamente se uma posição da grade já foi visitada.
     *
     * @param obj Objeto a ser comparado com este nó.
     * @return {@code true} se ambos representarem exatamente a mesma célula da matriz.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return col == node.col && row == node.row;
    }

    /**
     * Gera código hash consistente com {@link #equals(Object)} usando apenas {@code (col, row)},
     * garantindo consultas em tempo constante médio O(1) no {@code HashSet} do conjunto fechado.
     *
     * @return Código hash derivado da posição matricial.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(col, row);
    }

    /**
     * Retorna a coluna da célula na matriz do mapa.
     *
     * @return Índice de coluna (base 0).
     */
    public int getCol() {
        return col;
    }

    /**
     * Retorna a linha da célula na matriz do mapa.
     *
     * @return Índice de linha (base 0).
     */
    public int getRow() {
        return row;
    }

    /**
     * Retorna o custo real acumulado {@code G(n)} da origem até este nó.
     *
     * @return Custo acumulado ponderado por terreno.
     */
    public int getGCost() {
        return gCost;
    }

    /**
     * Atualiza o custo acumulado {@code G(n)} ao descobrir um trajeto válido até este vértice.
     *
     * @param gCost Novo custo real acumulado a partir do nó inicial.
     */
    public void setGCost(int gCost) {
        this.gCost = gCost;
    }

    /**
     * Retorna a estimativa heurística {@code H(n)} deste nó até o objetivo.
     *
     * @return Distância heurística calculada até o destino.
     */
    public int getHCost() {
        return hCost;
    }

    /**
     * Define a estimativa heurística {@code H(n)} até o alvo.
     *
     * @param hCost Valor heurístico calculado (distância de Manhattan).
     */
    public void setHCost(int hCost) {
        this.hCost = hCost;
    }

    /**
     * Retorna o número ordinal do passo após reconstrução da rota para exibição na HUD.
     *
     * @return Posição sequencial na rota (1 para o primeiro waypoint) ou 0 se não numerado.
     */
    public int getStepIndex() {
        return stepIndex;
    }

    /**
     * Atribui o índice ordinal do waypoint dentro da rota final reconstruída.
     *
     * @param stepIndex Ordem sequencial de visitação do passo.
     */
    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    /**
     * Retorna o nó antecessor na árvore de menor caminho para backtracking.
     *
     * @return Nó pai imediato, ou {@code null} para o vértice inicial.
     */
    public Node getParent() {
        return parent;
    }

    /**
     * Define o nó antecessor utilizado durante o relaxamento da aresta na busca A*.
     *
     * @param parent Nó pai através do qual se alcançou menor custo acumulado até aqui.
     */
    public void setParent(Node parent) {
        this.parent = parent;
    }
}
