package cloud.plataformatech.fadetoblack.ai;

/**
 * Representa um nó (vértice) na grade de busca do algoritmo A*.
 * Encapsula a posição matricial, custos ponderados (G, H e F) e
 * ponteiros de genealogia (parent) para reconstrução do caminho ótimo.
 */
public class Node implements Comparable<Node> {

    private final int col;
    private final int row;

    // Custo acumulado do ponto de partida até este nó
    private int gCost;

    // Estimativa heurística da distância deste nó até o alvo
    private int hCost;

    // Posição ordinal na rota reconstruída (para fins visuais e de telemetria)
    private int stepIndex = 0;

    // Referência ao nó antecessor na árvore de busca
    private Node parent;

    public Node(int col, int row) {
        this.col = col;
        this.row = row;
    }

    /**
     * Custo total estimado do nó: F = G + H.
     * O algoritmo A* sempre expande prioritariamente nós com menor F.
     */
    public int getFCost() {
        return gCost + hCost;
    }

    @Override
    public int compareTo(Node other) {
        int compare = Integer.compare(this.getFCost(), other.getFCost());
        if (compare == 0) {
            // Desempate heurístico: favorece o nó mais próximo do alvo
            compare = Integer.compare(this.hCost, other.hCost);
        }
        return compare;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return col == node.col && row == node.row;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(col, row);
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getGCost() {
        return gCost;
    }

    public void setGCost(int gCost) {
        this.gCost = gCost;
    }

    public int getHCost() {
        return hCost;
    }

    public void setHCost(int hCost) {
        this.hCost = hCost;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }
}
