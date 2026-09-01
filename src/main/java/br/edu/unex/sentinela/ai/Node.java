package br.edu.unex.sentinela.ai;

/**
 * A classe Node representa uma célula individual do mapa durante o processo de "raciocínio" do algoritmo A*.
 * Ele encapsula não apenas a posição geométrica da célula, mas as estimativas financeiras (custos)
 * que a Inteligência Artificial utilizará para julgar se esse caminho vale a pena.
 */
public class Node implements Comparable<Node> {
    
    // Coordenadas lógicas do tabuleiro (não são pixels absolutos, mas índices da matriz)
    private final int col;
    private final int row;

    // Custo G: Quanto já gastamos de "energia" caminhando do ponto inicial até este nó específico.
    private int gCost;
    
    // Custo H (Heurística): Um chute educado de quanta "energia" falta para chegar ao destino (usando distância de Manhattan).
    private int hCost;

    // A "migalha de pão" que aponta para o nó que pisamos imediatamente antes de chegar a este. 
    // É através desse encadeamento que reconstruiremos a rota final vitoriosa de trás para frente.
    private Node parent;

    /**
     * Construtor básico para alocação deste espaço analítico.
     * 
     * @param col Índice da coluna na matriz do mundo.
     * @param row Índice da linha na matriz do mundo.
     */
    public Node(int col, int row) {
        this.col = col;
        this.row = row;
    }

    /**
     * Custo F: O valor final absoluto deste nó, que define o quão "atraente" ele é para a inteligência artificial.
     * O A* sempre priorizará os nós com o menor Custo F.
     * 
     * @return A soma do esforço já gasto (G) com o esforço estimado restante (H).
     */
    public int getFCost() {
        return gCost + hCost;
    }

    /**
     * A interface "Comparable" exige a implementação desta rotina para que o algoritmo consiga 
     * colocar os nós em uma fila ordenada automaticamente do menor custo F para o maior.
     */
    @Override
    public int compareTo(Node other) {
        // Primeiramente, tenta desempatar pelo custo total da operação (FCost).
        int compare = Integer.compare(this.getFCost(), other.getFCost());
        if (compare == 0) {
            // Se os custos totais forem idênticos, dá preferência ao nó que estiver visualmente mais perto do alvo (HCost).
            compare = Integer.compare(this.hCost, other.hCost);
        }
        return compare;
    }

    /**
     * Verifica se dois nós representam exatamente a mesma célula física do mapa bidimensional.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return col == node.col && row == node.row;
    }

    /**
     * Gera uma assinatura digital baseada unicamente nas coordenadas, 
     * necessária para funcionamento otimizado em coleções como HashSet.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(col, row);
    }

    // --- Métodos de Leitura e Escrita (Getters / Setters) ---

    public int getCol() { return col; }
    public int getRow() { return row; }

    public int getGCost() { return gCost; }
    public void setGCost(int gCost) { this.gCost = gCost; }

    public int getHCost() { return hCost; }
    public void setHCost(int hCost) { this.hCost = hCost; }

    public Node getParent() { return parent; }
    public void setParent(Node parent) { this.parent = parent; }
}
