package cloud.plataformatech.fadetoblack.world;

import cloud.plataformatech.fadetoblack.entity.Enemy;
import cloud.plataformatech.fadetoblack.entity.EnemyArchetype;
import cloud.plataformatech.fadetoblack.entity.Player;
import cloud.plataformatech.fadetoblack.input.InputManager;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Núcleo do universo do jogo. Gerencia o ciclo de vida do mapa ativo,
 * as três entidades inimigas baseadas no algoritmo A*, o protagonista,
 * os portais de viagem dimensional e os obstáculos dinâmicos.
 */
public class GameWorld {

    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final TileMap tileMap;

    private boolean eKeyWasPressed = false;
    private String lastNotification = "Bem-vindo ao Project Fade to Black";
    private double notificationTimer = 5.0;

    public GameWorld() {
        this.tileMap = new TileMap(MapType.DUNGEON);
        this.player = new Player(4 * TileMap.TILE_SIZE, 4 * TileMap.TILE_SIZE);
        spawnEnemiesForMap(MapType.DUNGEON);
    }

    /**
     * Inicializa os 3 arquétipos de inimigos autônomos para o bioma selecionado.
     */
    private void spawnEnemiesForMap(MapType mapType) {
        enemies.clear();

        switch (mapType) {
            case DUNGEON -> {
                // Demônio no quadrante inferior direito
                enemies.add(new Enemy(EnemyArchetype.DEMON, 17 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, tileMap));
                // Morcego no quadrante superior direito
                enemies.add(new Enemy(EnemyArchetype.BAT, 16 * TileMap.TILE_SIZE, 2 * TileMap.TILE_SIZE, tileMap));
                // Espírito no corredor inferior esquerdo
                enemies.add(new Enemy(EnemyArchetype.GHOST, 2 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, tileMap));
            }
            case FOREST -> {
                // Demônio patrulhando a margem leste do rio
                enemies.add(new Enemy(EnemyArchetype.DEMON, 16 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, tileMap));
                // Morcego sobrevoando a clareira norte
                enemies.add(new Enemy(EnemyArchetype.BAT, 17 * TileMap.TILE_SIZE, 2 * TileMap.TILE_SIZE, tileMap));
                // Espírito espreitando na margem do rio
                enemies.add(new Enemy(EnemyArchetype.GHOST, 12 * TileMap.TILE_SIZE, 8 * TileMap.TILE_SIZE, tileMap));
            }
            case SNOW -> {
                // Demônio encurralando a passagem sudeste
                enemies.add(new Enemy(EnemyArchetype.DEMON, 16 * TileMap.TILE_SIZE, 11 * TileMap.TILE_SIZE, tileMap));
                // Morcego no topo nevado
                enemies.add(new Enemy(EnemyArchetype.BAT, 15 * TileMap.TILE_SIZE, 2 * TileMap.TILE_SIZE, tileMap));
                // Espírito flutuando sobre o abismo central
                enemies.add(new Enemy(EnemyArchetype.GHOST, 8 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, tileMap));
            }
        }
    }

    /**
     * Transição instantânea de mapa através de um portal ou comando de depuração.
     */
    public void changeMap(MapType newMap, int targetCol, int targetRow) {
        tileMap.loadMap(newMap);
        player.setPosition(targetCol * TileMap.TILE_SIZE, targetRow * TileMap.TILE_SIZE);
        player.triggerPortalCooldown();
        spawnEnemiesForMap(newMap);
        setNotification("Entrando em: " + newMap.getTitle());
    }

    /**
     * Ciclo de atualização global.
     */
    public void update(double deltaTime, InputManager input) {
        if (notificationTimer > 0) {
            notificationTimer -= deltaTime;
        }

        // 1. Atualização do mapa (obstáculos móveis e portais)
        boolean obstacleStateChanged = tileMap.update(deltaTime);

        // 2. Atualização física do jogador
        player.update(deltaTime, input, tileMap);

        // 3. Interação manual com obstáculos (Tecla E ou ESPAÇO)
        boolean ePressed = input.isKeyPressed(KeyCode.E) || input.isKeyPressed(KeyCode.SPACE);
        if (ePressed && !eKeyWasPressed) {
            toggleNearestObstacle();
        }
        eKeyWasPressed = ePressed;

        // 4. Verificação de Portais Dimensionais
        if (player.canTeleport()) {
            for (Portal portal : tileMap.getPortals()) {
                if (portal.touches(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                    changeMap(portal.getTargetMap(), portal.getTargetCol(), portal.getTargetRow());
                    break;
                }
            }
        }

        // 5. Atualização da IA dos 3 Inimigos
        for (Enemy enemy : enemies) {
            if (obstacleStateChanged) {
                // Se um portão abriu ou fechou, aciona o recálculo imediato do A*
                enemy.recalculatePath(player.getX(), player.getY());
            }
            enemy.update(deltaTime, player.getX(), player.getY());
        }
    }

    /**
     * Permite que o jogador acione manualmente um portão ou ponte próximo (alcance de 80px).
     */
    public void toggleNearestObstacle() {
        double px = player.getX() + player.getWidth() / 2.0;
        double py = player.getY() + player.getHeight() / 2.0;

        for (DynamicObstacle obs : tileMap.getDynamicObstacles()) {
            double obsCenterX = (obs.getCol() + obs.getWidthTiles() / 2.0) * TileMap.TILE_SIZE;
            double obsCenterY = (obs.getRow() + obs.getHeightTiles() / 2.0) * TileMap.TILE_SIZE;

            if (Math.hypot(px - obsCenterX, py - obsCenterY) < 100.0) {
                obs.toggle(tileMap);
                setNotification(obs.getType().getLabel() + " " + (obs.isOpen() ? "ABERTO" : "FECHADO") + " manualmente!");
                // Força recálculo imediato de todos os inimigos
                for (Enemy enemy : enemies) {
                    enemy.recalculatePath(player.getX(), player.getY());
                }
                break;
            }
        }
    }

    public void setNotification(String message) {
        this.lastNotification = message;
        this.notificationTimer = 3.5;
    }

    public String getLastNotification() {
        return notificationTimer > 0 ? lastNotification : null;
    }

    public TileMap getTileMap() {
        return tileMap;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    /**
     * Método retrocompatível com código antigo. Retorna o primeiro inimigo.
     */
    public Enemy getEnemy() {
        return enemies.isEmpty() ? null : enemies.get(0);
    }
}
