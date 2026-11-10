package cloud.plataformatech.fadetoblack.world;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orquestra a matriz bidimensional discreta (grade de tiles) do cenário ativo.
 *
 * <p>Mantém exatamente {@code 20 x 15} células de {@value #TILE_SIZE} px ({@code 800 x 600} pixels
 * lógicos em {@code GameApplication}), constrói a topografia estratégica de cada bioma para
 * evidenciar decisões de custo no algoritmo A* (desvios de piche, travessias por pontes alternadas
 * e escolhas entre gelo veloz vs. neve densa) e sincroniza obstáculos móveis e portais.</p>
 */
public class TileMap {

    /** Dimensão quadrada padrão de cada célula em coordenadas lógicas (pixels). */
    public static final int TILE_SIZE = 40;
    // Grade 20x15 preenchida 1:1 no viewport lógico 800x600 (20*40 = 800, 15*40 = 600)
    private final int cols = 20;
    private final int rows = 15;

    // Matriz bidimensional armazenando [coluna][linha]
    private final Tile[][] map;
    private MapType currentMapType;
    // Coleções de entidades estruturais associadas exclusivamente ao mapa atualmente carregado
    private final List<DynamicObstacle> dynamicObstacles = new ArrayList<>();
    private final List<Portal> portals = new ArrayList<>();

    /**
     * Inicializa o mapa carregando por padrão o bioma inicial {@link MapType#DUNGEON}.
     */
    public TileMap() {
        this(MapType.DUNGEON);
    }

    /**
     * Inicializa a grade e constrói imediatamente o bioma especificado.
     *
     * @param initialType Bioma inicial a ser carregado na criação do mundo.
     */
    public TileMap(MapType initialType) {
        this.map = new Tile[cols][rows];
        loadMap(initialType);
    }

    /**
     * Limpa elementos transitórios anteriores e reconstrói integralmente a topografia, os obstáculos
     * dinâmicos e os portais do bioma selecionado, sincronizando seu estado inicial sobre a matriz.
     *
     * @param mapType Bioma a construir na grade ativa.
     */
    public void loadMap(MapType mapType) {
        this.currentMapType = mapType;
        this.dynamicObstacles.clear();
        this.portals.clear();

        switch (mapType) {
            case FOREST -> buildForestMap();
            case SNOW -> buildSnowMap();
            case DUNGEON -> buildDungeonMap();
        }

        // Projeta os estados iniciais (ex.: portão fechado / ponte erguida) sobre os tiles afetados
        for (DynamicObstacle obstacle : dynamicObstacles) {
            obstacle.applyToMap(this);
        }
    }

    /**
     * Atualiza cronômetros de obstáculos mecânicos e animações de portais para o quadro atual.
     *
     * @param deltaTime Intervalo decorrido em segundos desde o quadro anterior.
     * @return {@code true} se ao menos um obstáculo alterou sua passabilidade neste quadro,
     *         sinalizando ao {@link GameWorld} para invalidar e recalcular imediatamente todas as rotas A*.
     */
    public boolean update(double deltaTime) {
        boolean anyStateChanged = false;

        for (DynamicObstacle obstacle : dynamicObstacles) {
            obstacle.update(deltaTime, this);
            if (obstacle.isStateChangedThisFrame()) {
                anyStateChanged = true;
            }
        }

        for (Portal portal : portals) {
            portal.update(deltaTime);
        }

        return anyStateChanged;
    }

    // ==========================================================================
    // 1. CONSTRUTOR DO MAPA: MASMORRA SOMBRIA
    // ==========================================================================
    /**
     * Monta o layout tático da Masmorra Sombria: corredores divididos por muralhas centrais,
     * bolsões estratégicos de piche (custo 4) e lama (custo 2) para forçar divergência de rotas
     * entre Demônio, Morcego e Espírito, além de portão central e porta deslizante alternados.
     */
    private void buildDungeonMap() {
        // Preenchimento base com moldura perimetral sólida de pedra
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (isBorder(c, r)) {
                    map[c][r] = new Tile(c, r, TerrainType.WALL);
                } else {
                    map[c][r] = new Tile(c, r, TerrainType.STONE_FLOOR);
                }
            }
        }

        // Estruturas internas em formato de câmaras interligadas por gargalos estreitos
        for (int r = 2; r <= 6; r++) map[6][r] = new Tile(6, r, TerrainType.WALL);
        for (int r = 8; r <= 12; r++) map[6][r] = new Tile(6, r, TerrainType.WALL);

        for (int c = 6; c <= 14; c++) map[c][7] = new Tile(c, 7, TerrainType.WALL);
        // Abre vão central na muralha (10,7) dedicado ao portão de aço principal
        map[10][7] = new Tile(10, 7, TerrainType.STONE_FLOOR);

        for (int r = 2; r <= 6; r++) map[14][r] = new Tile(14, r, TerrainType.WALL);
        for (int r = 9; r <= 13; r++) map[14][r] = new Tile(14, r, TerrainType.WALL);

        // Áreas de penalidade severa (Piche, custo 4): agentes terrestres tendem a contorná-las
        for (int c = 2; c <= 4; c++) {
            for (int r = 8; r <= 10; r++) {
                map[c][r] = new Tile(c, r, TerrainType.TAR);
            }
        }
        for (int c = 16; c <= 18; c++) {
            for (int r = 3; r <= 5; r++) {
                map[c][r] = new Tile(c, r, TerrainType.TAR);
            }
        }

        // Região intermediária de lamaçal (custo 2) na câmara sul
        for (int c = 8; c <= 12; c++) {
            for (int r = 10; r <= 12; r++) {
                map[c][r] = new Tile(c, r, TerrainType.MUD);
            }
        }

        // Obstáculos dinâmicos sincronizados em tempos assíncronos para forçar troca dinâmica de gargalos
        dynamicObstacles.add(new DynamicObstacle("gate_dungeon_main",
                DynamicObstacle.ObstacleType.GATE, 10, 7, 1, 1, false, 4.0, 3.5));

        dynamicObstacles.add(new DynamicObstacle("door_dungeon_east",
                DynamicObstacle.ObstacleType.SLIDING_DOOR, 14, 7, 1, 2, true, 3.5, 4.0));

        // Portais posicionados em cantos opostos dando acesso aos dois outros biomas
        portals.add(new Portal(1, 1, MapType.FOREST, 2, 7, "Floresta Corrompida", Color.web("#2d6a4f")));
        portals.add(new Portal(18, 13, MapType.SNOW, 3, 7, "Pico Nevado", Color.web("#48cae4")));

        map[1][1] = new Tile(1, 1, TerrainType.PORTAL);
        map[18][13] = new Tile(18, 13, TerrainType.PORTAL);
    }

    // ==========================================================================
    // 2. CONSTRUTOR DO MAPA: FLORESTA CORROMPIDA
    // ==========================================================================
    /**
     * Monta a Floresta Corrompida: um rio intransponível na coluna 10 corta o mapa de norte a sul,
     * tornando as duas pontes levadiças alternadas as únicas travessias possíveis e demonstrando
     * replanejamento completo de rota do A* entre os extremos norte e sul do rio.
     */
    private void buildForestMap() {
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (isBorder(c, r)) {
                    map[c][r] = new Tile(c, r, TerrainType.WALL);
                } else {
                    map[c][r] = new Tile(c, r, TerrainType.GRASS);
                }
            }
        }

        // Barreira fluvial vertical dividindo o mapa em duas margens na coluna 10
        for (int r = 1; r < rows - 1; r++) {
            map[10][r] = new Tile(10, r, TerrainType.WATER);
        }

        // Aglomerados naturais de vegetação sólida e paliçada oeste
        map[4][3] = new Tile(4, 3, TerrainType.WALL);
        map[4][4] = new Tile(4, 4, TerrainType.WALL);
        map[5][3] = new Tile(5, 3, TerrainType.WALL);

        map[15][9] = new Tile(15, 9, TerrainType.WALL);
        map[15][10] = new Tile(15, 10, TerrainType.WALL);
        map[16][10] = new Tile(16, 10, TerrainType.WALL);

        for (int r = 7; r <= 10; r++) map[5][r] = new Tile(5, r, TerrainType.WALL);
        // Passagem estreita na paliçada controlada por barricada deslizante
        map[5][8] = new Tile(5, 8, TerrainType.GRASS);

        // Zonas alagadiças de lamaçal (custo 2) nos dois lados da clareira
        for (int c = 2; c <= 4; c++) {
            for (int r = 10; r <= 13; r++) {
                map[c][r] = new Tile(c, r, TerrainType.MUD);
            }
        }
        for (int c = 12; c <= 15; c++) {
            for (int r = 2; r <= 5; r++) {
                map[c][r] = new Tile(c, r, TerrainType.MUD);
            }
        }

        // Poças tóxicas de piche (custo 4) na margem sudeste
        for (int c = 14; c <= 17; c++) {
            for (int r = 11; r <= 13; r++) {
                map[c][r] = new Tile(c, r, TerrainType.TAR);
            }
        }

        // Pontes levadiças em fase complementar sobre o rio (quando uma fecha, a outra tende a abrir)
        dynamicObstacles.add(new DynamicObstacle("bridge_north",
                DynamicObstacle.ObstacleType.DRAWBRIDGE, 10, 4, 1, 2, true, 5.0, 3.5));

        dynamicObstacles.add(new DynamicObstacle("bridge_south",
                DynamicObstacle.ObstacleType.DRAWBRIDGE, 10, 10, 1, 2, false, 4.5, 4.0));

        // Porta deslizante na paliçada ocidental
        dynamicObstacles.add(new DynamicObstacle("gate_forest_wood",
                DynamicObstacle.ObstacleType.SLIDING_DOOR, 5, 8, 1, 1, false, 3.5, 3.5));

        // Portais de retorno à Masmorra ou avanço para o Pico Nevado
        portals.add(new Portal(1, 13, MapType.DUNGEON, 4, 4, "Masmorra Sombria", Color.web("#800e13")));
        portals.add(new Portal(18, 1, MapType.SNOW, 3, 7, "Pico Nevado", Color.web("#48cae4")));

        map[1][13] = new Tile(1, 13, TerrainType.PORTAL);
        map[18][1] = new Tile(18, 1, TerrainType.PORTAL);
    }

    // ==========================================================================
    // 3. CONSTRUTOR DO MAPA: PICO NEVADO
    // ==========================================================================
    /**
     * Monta o Pico Nevado: explora o contraste físico entre pistas de gelo escorregadio (+35% com inércia),
     * bancos de neve profunda (custo 3, -65%) e um abismo glacial transponível apenas por ponte central.
     */
    private void buildSnowMap() {
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (isBorder(c, r)) {
                    map[c][r] = new Tile(c, r, TerrainType.WALL);
                } else {
                    map[c][r] = new Tile(c, r, TerrainType.SNOW_FLOOR);
                }
            }
        }

        // Fenda glacial central (coluna 10) criando gargalo estratégico no platô
        for (int r = 4; r <= 10; r++) {
            map[10][r] = new Tile(10, r, TerrainType.ABYSS);
        }

        // Formações rochosas e geleiras sólidas
        for (int c = 3; c <= 7; c++) map[c][3] = new Tile(c, 3, TerrainType.WALL);
        for (int c = 13; c <= 17; c++) map[c][11] = new Tile(c, 11, TerrainType.WALL);

        // Corredores de gelo polido (custo 1, +35% velocidade, ativa inércia no Player)
        for (int c = 4; c <= 8; c++) {
            for (int r = 6; r <= 8; r++) {
                map[c][r] = new Tile(c, r, TerrainType.ICE);
            }
        }
        for (int c = 12; c <= 16; c++) {
            for (int r = 6; r <= 8; r++) {
                map[c][r] = new Tile(c, r, TerrainType.ICE);
            }
        }

        // Acúmulos de neve densa (custo 3): exigem desvio estratégico pelos agentes terrestres
        for (int c = 2; c <= 5; c++) {
            for (int r = 10; r <= 13; r++) {
                map[c][r] = new Tile(c, r, TerrainType.DEEP_SNOW);
            }
        }
        for (int c = 14; c <= 18; c++) {
            for (int r = 2; r <= 4; r++) {
                map[c][r] = new Tile(c, r, TerrainType.DEEP_SNOW);
            }
        }

        // Passadiço retrátil central sobre o abismo e portão cristalino na rota norte
        dynamicObstacles.add(new DynamicObstacle("bridge_ice_abyss",
                DynamicObstacle.ObstacleType.DRAWBRIDGE, 10, 7, 1, 1, true, 4.0, 3.5));

        dynamicObstacles.add(new DynamicObstacle("gate_crystal_west",
                DynamicObstacle.ObstacleType.GATE, 5, 3, 1, 1, false, 3.5, 4.0));

        // Portais interligando de volta à Floresta Corrompida e à Masmorra Sombria
        portals.add(new Portal(1, 1, MapType.FOREST, 2, 7, "Floresta Corrompida", Color.web("#2d6a4f")));
        portals.add(new Portal(18, 13, MapType.DUNGEON, 4, 4, "Masmorra Sombria", Color.web("#800e13")));

        map[1][1] = new Tile(1, 1, TerrainType.PORTAL);
        map[18][13] = new Tile(18, 13, TerrainType.PORTAL);
    }

    /**
     * Verifica se a coordenada pertence à moldura externa do mapa para fechar o perímetro com muralhas.
     *
     * @param c Índice da coluna.
     * @param r Índice da linha.
     * @return {@code true} se estiver na primeira/última coluna ou primeira/última linha.
     */
    private boolean isBorder(int c, int r) {
        return c == 0 || c == cols - 1 || r == 0 || r == rows - 1;
    }

    /**
     * Recupera a célula na coordenada solicitada de forma segura.
     * Caso receba índices fora dos limites da matriz, retorna uma parede defensiva para impedir
     * {@link ArrayIndexOutOfBoundsException} durante testes de borda em colisões AABB.
     *
     * @param col Índice de coluna consultado.
     * @param row Índice de linha consultado.
     * @return Instância de {@link Tile} da posição ou uma célula sintética {@link TerrainType#WALL}.
     */
    public Tile getTile(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return new Tile(col, row, TerrainType.WALL);
        }
        return map[col][row];
    }

    /**
     * Retorna a largura total da grade em número de colunas.
     *
     * @return Número fixo de colunas (20).
     */
    public int getCols() {
        return cols;
    }

    /**
     * Retorna a altura total da grade em número de linhas.
     *
     * @return Número fixo de linhas (15).
     */
    public int getRows() {
        return rows;
    }

    /**
     * Retorna o bioma temático atualmente carregado na matriz.
     *
     * @return Constante {@link MapType} ativa.
     */
    public MapType getCurrentMapType() {
        return currentMapType;
    }

    /**
     * Retorna uma visão somente leitura dos obstáculos dinâmicos presentes no mapa atual,
     * protegendo a integridade estrutural da coleção interna contra modificações externas indevidas.
     *
     * @return Lista imutável de obstáculos dinâmicos.
     */
    public List<DynamicObstacle> getDynamicObstacles() {
        return Collections.unmodifiableList(dynamicObstacles);
    }

    /**
     * Retorna uma visão somente leitura dos portais dimensionais ativos neste mapa.
     *
     * @return Lista imutável de portais.
     */
    public List<Portal> getPortals() {
        return Collections.unmodifiableList(portals);
    }
}
