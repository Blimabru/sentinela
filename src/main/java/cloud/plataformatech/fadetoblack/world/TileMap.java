package cloud.plataformatech.fadetoblack.world;

/**
 * A classe TileMap orquestra a grade espacial invisível que forma a "planta baixa" da fase do jogo.
 * É através de uma Matriz (Array Bidimensional) que traduzimos as imagens visuais da tela em dados
 * de coordenadas para as fórmulas matemáticas do computador entenderem limites e obstáculos.
 */
public class TileMap {
    
    // A dimensão constante e imutável de cada bloco na tela, neste caso, 40 por 40 pixels.
    public static final int TILE_SIZE = 40;
    
    // A quantidade matemática de colunas (largura) que preencherá o tabuleiro virtual do jogo.
    private final int cols = 20;
    
    // A quantidade matemática de linhas (altura) que comporá o tabuleiro virtual do jogo.
    private final int rows = 15;
    
    // A matriz central onde cada posição x, y contém um objeto complexo do tipo "Tile" detalhando suas propriedades.
    private final Tile[][] map;

    /**
     * Durante a inicialização do mapa, a matriz de instâncias de "Tile" é alocada na memória
     * e os blocos básicos são configurados metodicamente.
     */
    public TileMap() {
        map = new Tile[cols][rows];
        generateMap();
    }

    /**
     * O processo interno que constrói as paredes limitadoras e a área útil. 
     * Ele lê as coordenadas numéricas para inserir objetos "Tile" específicos baseados nas regras.
     */
    private void generateMap() {
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                
                // Estabelece uma cerca ao redor de todo o mapa: se for a primeira/última linha ou coluna, cria Parede.
                if (c == 0 || c == cols - 1 || r == 0 || r == rows - 1) {
                    // Instancia um tile do tipo Parede (tipo 1, intransitável (false), custo de movimento nulo pois é inalcançável).
                    map[c][r] = new Tile(1, false, 0);
                } 
                // Adiciona trechos de lama estáticos para introduzir a mecânica de diferentes custos de locomoção.
                else if (c > 5 && c < 10 && r > 5 && r < 10) {
                    // Instancia um tile do tipo Lama (tipo 2, transitável (true), custo de movimento encarecido para 2).
                    map[c][r] = new Tile(2, true, 2);
                } 
                // Para todo o restante das posições vazias da matriz, aplica-se o terreno padrão.
                else {
                    // Instancia um tile do tipo Chão Limpo (tipo 0, transitável (true), custo de movimento ideal de 1).
                    map[c][r] = new Tile(0, true, 1);
                }
            }
        }
    }

    /**
     * Procura de maneira segura qual tipo de terreno (Tile) existe em coordenadas virtuais específicas.
     * Caso os agentes tentem ler fora do alcance permitido, retorna uma barreira inquebrável por garantia de segurança.
     * 
     * @param col Índice da coluna (eixo X lógico).
     * @param row Índice da linha (eixo Y lógico).
     * @return O objeto Tile correspondente ou um Tile intransitável se estiver fora dos limites.
     */
    public Tile getTile(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return new Tile(1, false, 0); // Proteção contra travamentos (NullPointerException)
        }
        return map[col][row];
    }

    /**
     * Método de acesso analítico para extrair o número de colunas.
     * @return Quantidade de colunas instanciadas no mapa.
     */
    public int getCols() {
        return cols;
    }

    /**
     * Método de acesso analítico para extrair o número de linhas.
     * @return Quantidade de linhas instanciadas no mapa.
     */
    public int getRows() {
        return rows;
    }
}
