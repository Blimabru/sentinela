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
 * Fachada central do domínio de simulação do jogo.
 *
 * <p>Orquestra o estado completo do universo ativo: grade espacial ({@link TileMap}), entidade
 * controlada pelo usuário ({@link Player}), equipe de três agentes autônomos ({@link Enemy}),
 * transições entre biomas via portais, interação manual com obstáculos dinâmicos e sistema de
 * notificações flutuantes na HUD.</p>
 */
public class GameWorld {

    // Protagonista controlado pelo usuário
    private final Player player;
    // Conjunto ativo dos três arquétipos de IA perseguindo o jogador simultaneamente
    private final List<Enemy> enemies = new ArrayList<>();
    // Matriz do mapa corrente com bioma, terrenos, obstáculos e portais
    private final TileMap tileMap;

    // Trava edge-trigger para evitar disparos contínuos ao manter E ou ESPAÇO pressionados
    private boolean eKeyWasPressed = false;
    // Mensagem de aviso temporária renderizada em destaque na HUD superior
    private String lastNotification = "Bem-vindo ao Project Fade to Black";
    private double notificationTimer = 5.0;

    /**
     * Inicializa o mundo de jogo posicionando o jogador no bioma inicial ({@link MapType#DUNGEON})
     * e gerando a primeira formação dos três arquétipos de inimigos autônomos.
     */
    public GameWorld() {
        this.tileMap = new TileMap(MapType.DUNGEON);
        this.player = new Player(4 * TileMap.TILE_SIZE, 4 * TileMap.TILE_SIZE);
        spawnEnemiesForMap(MapType.DUNGEON);
    }

    /**
     * Limpa a lista anterior e posiciona os três arquétipos ({@code DEMON}, {@code BAT}, {@code GHOST})
     * em coordenadas estratégicas próprias de cada bioma para evidenciar divergências de rota no A*.
     *
     * @param mapType Bioma recém-carregado onde os agentes serão instanciados.
     */
    private void spawnEnemiesForMap(MapType mapType) {
        enemies.clear();

        switch (mapType) {
            case DUNGEON -> {
                // Demônio no quadrante inferior direito (pressiona pelo corredor sul com lama)
                enemies.add(new Enemy(EnemyArchetype.DEMON, 17 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, tileMap));
                // Morcego no quadrante superior direito (sobrevoa região nordeste com piche)
                enemies.add(new Enemy(EnemyArchetype.BAT, 16 * TileMap.TILE_SIZE, 2 * TileMap.TILE_SIZE, tileMap));
                // Espírito no corredor inferior esquerdo (atravessa piche sem penalidade)
                enemies.add(new Enemy(EnemyArchetype.GHOST, 2 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, tileMap));
            }
            case FOREST -> {
                // Demônio patrulhando a margem leste do rio
                enemies.add(new Enemy(EnemyArchetype.DEMON, 16 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, tileMap));
                // Morcego sobrevoando a clareira norte
                enemies.add(new Enemy(EnemyArchetype.BAT, 17 * TileMap.TILE_SIZE, 2 * TileMap.TILE_SIZE, tileMap));
                // Espírito espreitando próximo à margem central para cruzar a ponte mais próxima
                enemies.add(new Enemy(EnemyArchetype.GHOST, 12 * TileMap.TILE_SIZE, 8 * TileMap.TILE_SIZE, tileMap));
            }
            case SNOW -> {
                // Demônio encurralando a passagem sudeste
                enemies.add(new Enemy(EnemyArchetype.DEMON, 16 * TileMap.TILE_SIZE, 11 * TileMap.TILE_SIZE, tileMap));
                // Morcego no topo nevado (mitiga parcialmente a neve profunda)
                enemies.add(new Enemy(EnemyArchetype.BAT, 15 * TileMap.TILE_SIZE, 2 * TileMap.TILE_SIZE, tileMap));
                // Espírito próximo ao setor sul com neve densa, imune ao atolamento
                enemies.add(new Enemy(EnemyArchetype.GHOST, 8 * TileMap.TILE_SIZE, 12 * TileMap.TILE_SIZE, tileMap));
            }
        }
    }

    /**
     * Executa uma transição imediata de cenário (disparada por portais, tecla {@code T} ou modo de captura).
     * Reconstrói o mapa, reposiciona o jogador zerando sua inércia, ativa cooldown de portal,
     * recria os inimigos e exibe notificação visual na HUD.
     *
     * @param newMap    Bioma de destino a carregar.
     * @param targetCol Coluna de surgimento do protagonista no novo cenário.
     * @param targetRow Linha de surgimento do protagonista no novo cenário.
     */
    public void changeMap(MapType newMap, int targetCol, int targetRow) {
        tileMap.loadMap(newMap);
        player.setPosition(targetCol * TileMap.TILE_SIZE, targetRow * TileMap.TILE_SIZE);
        // Evita disparo em cascata caso o ponto de chegada fique próximo a outro portal
        player.triggerPortalCooldown();
        spawnEnemiesForMap(newMap);
        setNotification("Entrando em: " + newMap.getTitle());
    }

    /**
     * Avança todas as regras de simulação do mundo em ordem determinística para o quadro corrente.
     *
     * @param deltaTime Intervalo de simulação do quadro em segundos.
     * @param input     Gerenciador de teclado consultado para movimentação e interação com obstáculos.
     */
    public void update(double deltaTime, InputManager input) {
        if (notificationTimer > 0) {
            notificationTimer -= deltaTime;
        }

        // 1. Atualiza ciclos de obstáculos móveis e registra se houve abertura/fechamento topológico
        boolean obstacleStateChanged = tileMap.update(deltaTime);

        // 2. Processa movimentação, física de terreno e colisões AABB do protagonista
        player.update(deltaTime, input, tileMap);

        // 3. Detecta acionamento manual de obstáculo próximo via borda de subida em [E] ou [ESPAÇO]
        boolean ePressed = input.isKeyPressed(KeyCode.E) || input.isKeyPressed(KeyCode.SPACE);
        if (ePressed && !eKeyWasPressed) {
            toggleNearestObstacle();
        }
        eKeyWasPressed = ePressed;

        // 4. Verifica colisão central com portais interdimensionais respeitando o cooldown pós-viagem
        if (player.canTeleport()) {
            for (Portal portal : tileMap.getPortals()) {
                if (portal.touches(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                    changeMap(portal.getTargetMap(), portal.getTargetCol(), portal.getTargetRow());
                    break;
                }
            }
        }

        // 5. Atualiza os agentes de IA; caso algum portão/ponte tenha mudado neste quadro,
        // força o replanejamento imediato do A* antes mesmo de vencer o timer do arquétipo
        for (Enemy enemy : enemies) {
            if (obstacleStateChanged) {
                enemy.recalculatePath(player.getX(), player.getY());
            }
            enemy.update(deltaTime, player.getX(), player.getY());
        }
    }

    /**
     * Permite ao jogador acionar manualmente o primeiro portão, porta deslizante ou ponte levadiça
     * encontrado dentro do raio euclidiano de interação ({@code 100 px} entre os centros).
     * Após alternar o estado, invalida imediatamente as rotas de todos os inimigos ativos.
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
                // Replaneja todas as rotas no mesmo quadro para demonstração instantânea do A* reativo
                for (Enemy enemy : enemies) {
                    enemy.recalculatePath(player.getX(), player.getY());
                }
                break;
            }
        }
    }

    /**
     * Agenda uma notificação textual temporária na faixa superior do HUD por 3,5 segundos.
     *
     * @param message Texto informativo a exibir ao usuário.
     */
    public void setNotification(String message) {
        this.lastNotification = message;
        this.notificationTimer = 3.5;
    }

    /**
     * Retorna a notificação ativa enquanto seu temporizador estiver positivo.
     *
     * @return Texto da notificação vigente, ou {@code null} após expiração do tempo.
     */
    public String getLastNotification() {
        return notificationTimer > 0 ? lastNotification : null;
    }

    /**
     * Retorna o mapa bidimensional atualmente gerenciado pelo mundo.
     *
     * @return Instância ativa de {@link TileMap}.
     */
    public TileMap getTileMap() {
        return tileMap;
    }

    /**
     * Retorna a instância única do protagonista.
     *
     * @return Jogador controlado pelo usuário.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Retorna visão imutável dos agentes de IA em perseguição ativa no bioma atual.
     *
     * @return Lista somente leitura de inimigos.
     */
    public List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    /**
     * Método auxiliar retrocompatível com testes antigos para recuperar o primeiro inimigo da lista.
     *
     * @return Primeiro {@link Enemy} ativo ou {@code null} se a lista estiver vazia.
     */
    public Enemy getEnemy() {
        return enemies.isEmpty() ? null : enemies.get(0);
    }
}
