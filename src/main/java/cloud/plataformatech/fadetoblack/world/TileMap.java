package cloud.plataformatech.fadetoblack.world;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orquestra a grade espacial bidimensional do mapa atual.
 * Constrói os biomas temáticos (Masmorra, Floresta e Neve), gerencia
 * os tipos de terrenos com custos distintos de travessia, abriga os
 * obstáculos dinâmicos (portas, portões e pontes) e os portais dimensionais.
 */
public class TileMap {

    public static final int TILE_SIZE = 40;
    private final int cols = 20;
    private final int rows = 15;

    private final Tile[][] map;
    private MapType currentMapType;
    private final List<DynamicObstacle> dynamicObstacles = new ArrayList<>();
    private final List<Portal> portals = new ArrayList<>();

    public TileMap() {
        this(MapType.DUNGEON);
    }

    public TileMap(MapType initialType) {
        this.map = new Tile[cols][rows];
        loadMap(initialType);
    }

    /**
     * Constrói e inicializa a topografia do bioma especificado.
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

        // Aplica o estado inicial de todos os obstáculos sobre a malha de tiles
        for (DynamicObstacle obstacle : dynamicObstacles) {
            obstacle.applyToMap(this);
        }
    }

    /**
     * Atualiza obstáculos móveis e animações de portais.
     * @return true se algum obstáculo mudou de estado neste quadro.
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

    // ==========================================
    // 1. CONSTRUTOR DO MAPA: MASMORRA SOMBRIA
    // ==========================================
    private void buildDungeonMap() {
        // Base de pedra
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (isBorder(c, r)) {
                    map[c][r] = new Tile(c, r, TerrainType.WALL);
                } else {
                    map[c][r] = new Tile(c, r, TerrainType.STONE_FLOOR);
                }
            }
        }

        // Paredes e corredores internos da masmorra
        for (int r = 2; r <= 6; r++) map[6][r] = new Tile(6, r, TerrainType.WALL);
        for (int r = 8; r <= 12; r++) map[6][r] = new Tile(6, r, TerrainType.WALL);

        for (int c = 6; c <= 14; c++) map[c][7] = new Tile(c, 7, TerrainType.WALL);
        map[10][7] = new Tile(10, 7, TerrainType.STONE_FLOOR); // Passagem para o portão principal

        for (int r = 2; r <= 6; r++) map[14][r] = new Tile(14, r, TerrainType.WALL);
        for (int r = 9; r <= 13; r++) map[14][r] = new Tile(14, r, TerrainType.WALL);

        // Terrenos com penalidade: Piche viscoso (custo 4)
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

        // Terrenos com penalidade: Lamaçal da masmorra (custo 2)
        for (int c = 8; c <= 12; c++) {
            for (int r = 10; r <= 12; r++) {
                map[c][r] = new Tile(c, r, TerrainType.MUD);
            }
        }

        // Obstáculos Dinâmicos
        // Portão 1: Central no corredor da masmorra (Abre e fecha a cada 4s/3s)
        dynamicObstacles.add(new DynamicObstacle("gate_dungeon_main",
                DynamicObstacle.ObstacleType.GATE, 10, 7, 1, 1, false, 4.0, 3.5));

        // Portão 2: Corredor leste (Porta deslizante)
        dynamicObstacles.add(new DynamicObstacle("door_dungeon_east",
                DynamicObstacle.ObstacleType.SLIDING_DOOR, 14, 7, 1, 2, true, 3.5, 4.0));

        // Portais Dimensionais
        portals.add(new Portal(1, 1, MapType.FOREST, 2, 7, "Floresta Corrompida", Color.web("#2d6a4f")));
        portals.add(new Portal(18, 13, MapType.SNOW, 3, 7, "Pico Nevado", Color.web("#48cae4")));

        map[1][1] = new Tile(1, 1, TerrainType.PORTAL);
        map[18][13] = new Tile(18, 13, TerrainType.PORTAL);
    }

    // ==========================================
    // 2. CONSTRUTOR DO MAPA: FLORESTA CORROMPIDA
    // ==========================================
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

        // Rio que corta a floresta verticalmente (coluna 10)
        for (int r = 1; r < rows - 1; r++) {
            map[10][r] = new Tile(10, r, TerrainType.WATER);
        }

        // Grupos de árvores intransponíveis (muralhas vegetais)
        map[4][3] = new Tile(4, 3, TerrainType.WALL);
        map[4][4] = new Tile(4, 4, TerrainType.WALL);
        map[5][3] = new Tile(5, 3, TerrainType.WALL);

        map[15][9] = new Tile(15, 9, TerrainType.WALL);
        map[15][10] = new Tile(15, 10, TerrainType.WALL);
        map[16][10] = new Tile(16, 10, TerrainType.WALL);

        for (int r = 7; r <= 10; r++) map[5][r] = new Tile(5, r, TerrainType.WALL);
        map[5][8] = new Tile(5, 8, TerrainType.GRASS); // Passagem com porta de troncos

        // Pântanos de lama (custo 2)
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

        // Poças venenosas de piche (custo 4)
        for (int c = 14; c <= 17; c++) {
            for (int r = 11; r <= 13; r++) {
                map[c][r] = new Tile(c, r, TerrainType.TAR);
            }
        }

        // Pontes Levadiças sobre o rio
        // Ponte 1 Norte: Aberta por 5s, fecha (ergue) por 3.5s
        dynamicObstacles.add(new DynamicObstacle("bridge_north",
                DynamicObstacle.ObstacleType.DRAWBRIDGE, 10, 4, 1, 2, true, 5.0, 3.5));

        // Ponte 2 Sul: Começa erguida (fechada) por 4s, abaixa (abre) por 4.5s
        dynamicObstacles.add(new DynamicObstacle("bridge_south",
                DynamicObstacle.ObstacleType.DRAWBRIDGE, 10, 10, 1, 2, false, 4.5, 4.0));

        // Barricada móvel de troncos deslizante
        dynamicObstacles.add(new DynamicObstacle("gate_forest_wood",
                DynamicObstacle.ObstacleType.SLIDING_DOOR, 5, 8, 1, 1, false, 3.5, 3.5));

        // Portais Dimensionais
        portals.add(new Portal(1, 13, MapType.DUNGEON, 4, 4, "Masmorra Sombria", Color.web("#800e13")));
        portals.add(new Portal(18, 1, MapType.SNOW, 3, 7, "Pico Nevado", Color.web("#48cae4")));

        map[1][13] = new Tile(1, 13, TerrainType.PORTAL);
        map[18][1] = new Tile(18, 1, TerrainType.PORTAL);
    }

    // ==========================================
    // 3. CONSTRUTOR DO MAPA: PICO NEVADO
    // ==========================================
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

        // Abismo glacial intransponível cortando parte do platô
        for (int r = 4; r <= 10; r++) {
            map[10][r] = new Tile(10, r, TerrainType.ABYSS);
        }

        // Rochas e geleiras sólidas
        for (int c = 3; c <= 7; c++) map[c][3] = new Tile(c, 3, TerrainType.WALL);
        for (int c = 13; c <= 17; c++) map[c][11] = new Tile(c, 11, TerrainType.WALL);

        // Faixas de Gelo Escorregadio (alta velocidade, custo 1)
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

        // Neve Profunda (alta lentidão, custo 3)
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

        // Obstáculo Dinâmico: Ponte de Gelo / Mecanismo Retrátil sobre o abismo
        dynamicObstacles.add(new DynamicObstacle("bridge_ice_abyss",
                DynamicObstacle.ObstacleType.DRAWBRIDGE, 10, 7, 1, 1, true, 4.0, 3.5));

        // Portão de Cristal Glacial na passagem oeste
        dynamicObstacles.add(new DynamicObstacle("gate_crystal_west",
                DynamicObstacle.ObstacleType.GATE, 5, 3, 1, 1, false, 3.5, 4.0));

        // Portais Dimensionais
        portals.add(new Portal(1, 1, MapType.FOREST, 2, 7, "Floresta Corrompida", Color.web("#2d6a4f")));
        portals.add(new Portal(18, 13, MapType.DUNGEON, 4, 4, "Masmorra Sombria", Color.web("#800e13")));

        map[1][1] = new Tile(1, 1, TerrainType.PORTAL);
        map[18][13] = new Tile(18, 13, TerrainType.PORTAL);
    }

    private boolean isBorder(int c, int r) {
        return c == 0 || c == cols - 1 || r == 0 || r == rows - 1;
    }

    public Tile getTile(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return new Tile(col, row, TerrainType.WALL);
        }
        return map[col][row];
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public MapType getCurrentMapType() {
        return currentMapType;
    }

    public List<DynamicObstacle> getDynamicObstacles() {
        return Collections.unmodifiableList(dynamicObstacles);
    }

    public List<Portal> getPortals() {
        return Collections.unmodifiableList(portals);
    }
}
