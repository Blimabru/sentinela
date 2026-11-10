package cloud.plataformatech.fadetoblack.entity;

import cloud.plataformatech.fadetoblack.ai.AStarPathfinder;
import cloud.plataformatech.fadetoblack.ai.Node;
import cloud.plataformatech.fadetoblack.world.Tile;
import cloud.plataformatech.fadetoblack.world.TileMap;

import java.util.List;

/**
 * Entidade de Inteligência Artificial autônoma responsável por perseguir o jogador.
 *
 * <p>Cada agente instancia seu próprio {@link AStarPathfinder} parametrizado pelas regras de
 * {@link EnemyArchetype}, combina replanejamento periódico com detecção antecipada de bloqueios
 * em obstáculos dinâmicos e publica telemetria completa para o painel overhead.</p>
 */
public class Enemy {

    // Perfil comportamental, biológico e estético deste agente
    private final EnemyArchetype archetype;
    // Coordenadas de canto superior esquerdo no espaço lógico (800x600)
    private double x;
    private double y;
    // Dimensões reduzidas em relação ao tile (32 vs 40 px) para garantir folga de 4 px nas bordas de corredores
    private final double width = 32.0;
    private final double height = 32.0;

    // Referência ao ambiente e motor exclusivo de busca heurística desta entidade
    private final TileMap tileMap;
    private final AStarPathfinder pathfinder;
    private List<Node> currentPath;

    // Acumuladores temporais para cadência de recálculo A* e oscilação de sprites animados
    private double pathCalculationTimer = 0.0;
    private double animationTimer = 0.0;

    // --- Métricas de Telemetria e Diagnóstico da IA ---
    private String aiState = "INICIANDO";
    private double currentSpeed = 0.0;
    private double currentSlowPercent = 0.0;
    private String currentTerrainName = "Pedra";
    private int currentTerrainCost = 1;
    private double astarLatencyMs = 0.0;
    private int nodesExplored = 0;
    private int pathCost = 0;
    private int heuristicDistance = 0;

    /**
     * Construtor retrocompatível que cria um agente padrão do arquétipo {@link EnemyArchetype#DEMON}.
     *
     * @param startX  Posição horizontal inicial em pixels.
     * @param startY  Posição vertical inicial em pixels.
     * @param tileMap Referência à grade do mapa ativo.
     */
    public Enemy(double startX, double startY, TileMap tileMap) {
        this(EnemyArchetype.DEMON, startX, startY, tileMap);
    }

    /**
     * Inicializa um agente autônomo configurando posição inicial, motor A* e disparo imediato de rota.
     *
     * @param archetype Arquétipo biológico e comportamental do inimigo.
     * @param startX    Posição horizontal inicial em pixels.
     * @param startY    Posição vertical inicial em pixels.
     * @param tileMap   Mapa de referência para consultas de colisão e navegação.
     */
    public Enemy(EnemyArchetype archetype, double startX, double startY, TileMap tileMap) {
        this.archetype = archetype;
        this.x = startX;
        this.y = startY;
        this.tileMap = tileMap;
        this.pathfinder = new AStarPathfinder(tileMap);
        this.currentSpeed = archetype.getBaseSpeed();
        // Pré-carrega o timer no limite de atraso para forçar o cálculo da primeira rota logo no 1º tick
        this.pathCalculationTimer = archetype.getRecalculationDelay();
    }

    /**
     * Executa o ciclo de percepção, decisão e atuação da IA para o quadro corrente.
     *
     * @param deltaTime Tempo decorrido desde o quadro anterior em segundos.
     * @param targetX   Coordenada X do alvo (jogador) em pixels.
     * @param targetY   Coordenada Y do alvo (jogador) em pixels.
     */
    public void update(double deltaTime, double targetX, double targetY) {
        // Multiplica por 5.0 rad/s para obter batimento de asas e flutuação fluidos no Renderer
        animationTimer += deltaTime * 5.0;
        pathCalculationTimer += deltaTime;

        // Amostra o tile exatamente sob o centro geométrico da hitbox do agente
        int currentTileCol = (int) ((this.x + width / 2.0) / TileMap.TILE_SIZE);
        int currentTileRow = (int) ((this.y + height / 2.0) / TileMap.TILE_SIZE);
        Tile currentTile = tileMap.getTile(currentTileCol, currentTileRow);

        // Avalia atrito físico e registra indicadores de terreno para a telemetria overhead
        double terrainSpeedFactor = archetype.calculateSpeedFactor(currentTile);
        this.currentTerrainName = currentTile.getEffectiveType().getDisplayName();
        this.currentTerrainCost = archetype.modifyTerrainCost(currentTile);
        this.currentSlowPercent = (1.0 - terrainSpeedFactor) * 100.0;

        // Aplica regra tática exclusiva do arquétipo sobre a velocidade base
        double effectiveBaseSpeed = archetype.getBaseSpeed();
        double distToPlayer = Math.hypot((targetX - this.x), (targetY - this.y));

        if (archetype == EnemyArchetype.DEMON) {
            // Máquina de estados tática do Demônio: ao entrar em alcance de emboscada (< 160 px),
            // ativa estado de Fúria Melee com bônus de +30% sobre a velocidade nominal
            if (distToPlayer < 160.0) {
                effectiveBaseSpeed *= 1.30;
                this.aiState = "FÚRIA MELEE";
            } else {
                this.aiState = "PERSEGUINDO";
            }
        } else if (archetype == EnemyArchetype.BAT) {
            this.aiState = "ECO-CAÇADA";
        } else if (archetype == EnemyArchetype.GHOST) {
            this.aiState = "FLUTUANDO";
        }

        // Velocidade linear resultante combinando estado tático e penalidade do terreno
        this.currentSpeed = effectiveBaseSpeed * terrainSpeedFactor;

        // Replanejamento reativo + periódico: força recálculo imediato caso um obstáculo bloqueie os
        // próximos nós antes mesmo de expirar o intervalo periódico do arquétipo
        if (isCurrentPathBlocked()) {
            recalculatePath(targetX, targetY);
        } else if (pathCalculationTimer >= archetype.getRecalculationDelay()) {
            recalculatePath(targetX, targetY);
        }

        // Converte os waypoints discretos do grafo em movimento vetorial contínuo
        followPath(deltaTime);
    }

    /**
     * Invoca diretamente o algoritmo A* e atualiza as métricas de desempenho armazenadas nesta entidade.
     * Chamado periodicamente em {@link #update} ou imediatamente pelo {@code GameWorld} quando
     * um portão/ponte altera a topologia do mapa.
     *
     * @param targetX Posição X de destino em pixels.
     * @param targetY Posição Y de destino em pixels.
     */
    public void recalculatePath(double targetX, double targetY) {
        pathCalculationTimer = 0.0;

        // Converte os centros geométricos de origem e alvo do espaço contínuo (px) para índices da grade
        int startCol = (int) ((this.x + width / 2.0) / TileMap.TILE_SIZE);
        int startRow = (int) ((this.y + height / 2.0) / TileMap.TILE_SIZE);

        int targetCol = (int) ((targetX + width / 2.0) / TileMap.TILE_SIZE);
        int targetRow = (int) ((targetY + height / 2.0) / TileMap.TILE_SIZE);

        // Injeta via expressão lambda o modificador biológico do arquétipo no motor A*
        currentPath = pathfinder.findPath(startCol, startRow, targetCol, targetRow,
                tile -> archetype.modifyTerrainCost(tile));

        // Sincroniza instantaneamente os contadores de telemetria pós-busca
        this.astarLatencyMs = pathfinder.getLastCalculationTimeMs();
        this.nodesExplored = pathfinder.getNodesExplored();
        this.pathCost = pathfinder.getLastPathCost();
        this.heuristicDistance = pathfinder.getLastInitialHCost();
    }

    /**
     * Realiza inspeção antecipada (lookahead) nos próximos 3 waypoints da rota planejada para detectar
     * se algum obstáculo dinâmico (portão ou ponte) fechou subitamente à frente do agente.
     *
     * @return {@code true} se ao menos um dos 3 próximos nós estiver intransponível; {@code false} caso contrário.
     */
    private boolean isCurrentPathBlocked() {
        if (currentPath == null || currentPath.isEmpty()) return false;
        int lookahead = Math.min(3, currentPath.size());
        for (int i = 0; i < lookahead; i++) {
            Node node = currentPath.get(i);
            Tile t = tileMap.getTile(node.getCol(), node.getRow());
            if (!t.isWalkable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executa o seguimento de trajetória (waypoint steering) em direção ao próximo nó do A*.
     * Normaliza o vetor direção para manter velocidade escalar constante e remove o nó da fila ao
     * aproximar-se dentro da tolerância de chegada ({@code <= 3.0 px}), prevenindo oscilação sobre o centro.
     *
     * @param deltaTime Intervalo de simulação do quadro em segundos.
     */
    private void followPath(double deltaTime) {
        if (currentPath == null || currentPath.isEmpty()) {
            return;
        }

        Node nextStep = currentPath.get(0);
        // Calcula a posição superior esquerda que alinha exatamente o centro da entidade ao centro do tile alvo
        double targetPixelX = (nextStep.getCol() * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE - width) / 2.0;
        double targetPixelY = (nextStep.getRow() * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE - height) / 2.0;

        double dx = targetPixelX - this.x;
        double dy = targetPixelY - this.y;
        double distance = Math.hypot(dx, dy);

        if (distance > 3.0) {
            // Vetor unitário (dx/distance, dy/distance) garante velocidade exatamente igual a currentSpeed
            this.x += (dx / distance) * currentSpeed * deltaTime;
            this.y += (dy / distance) * currentSpeed * deltaTime;
        } else {
            // Waypoint alcançado: consome a cabeça da fila para avançar imediatamente ao próximo passo
            currentPath.remove(0);
        }
    }

    /**
     * Reposiciona o agente abruptamente (por exemplo, após transições de mapa) e invalida a rota antiga.
     *
     * @param newX Nova coordenada horizontal em pixels.
     * @param newY Nova coordenada vertical em pixels.
     */
    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        this.currentPath = null;
        this.pathCalculationTimer = archetype.getRecalculationDelay();
    }

    // --- Getters de Renderização e Métricas ---

    /**
     * Retorna o arquétipo definidor deste inimigo.
     *
     * @return Instância de {@link EnemyArchetype}.
     */
    public EnemyArchetype getArchetype() {
        return archetype;
    }

    /**
     * Retorna a coordenada horizontal do canto superior esquerdo em pixels.
     *
     * @return Posição X lógica.
     */
    public double getX() {
        return x;
    }

    /**
     * Retorna a coordenada vertical do canto superior esquerdo em pixels.
     *
     * @return Posição Y lógica.
     */
    public double getY() {
        return y;
    }

    /**
     * Retorna a largura da hitbox do agente em pixels.
     *
     * @return Largura padrão (32 px).
     */
    public double getWidth() {
        return width;
    }

    /**
     * Retorna a altura da hitbox do agente em pixels.
     *
     * @return Altura padrão (32 px).
     */
    public double getHeight() {
        return height;
    }

    /**
     * Retorna a lista de nós restante da rota A* atualmente seguida.
     *
     * @return Lista de waypoints ou {@code null} se ainda não calculada.
     */
    public List<Node> getCurrentPath() {
        return currentPath;
    }

    /**
     * Retorna o rótulo do estado comportamental atual (ex.: {@code "FÚRIA MELEE"}, {@code "ECO-CAÇADA"}).
     *
     * @return Texto identificador do estado tático da IA.
     */
    public String getAiState() {
        return aiState;
    }

    /**
     * Retorna a velocidade efetiva final em pixels por segundo neste quadro.
     *
     * @return Velocidade escalar corrente.
     */
    public double getCurrentSpeed() {
        return currentSpeed;
    }

    /**
     * Retorna a redução percentual de velocidade causada pelo terreno atual.
     *
     * @return Percentual de lentidão entre 0.0 e 100.0.
     */
    public double getCurrentSlowPercent() {
        return currentSlowPercent;
    }

    /**
     * Retorna o nome legível do terreno sob os pés do agente.
     *
     * @return Nome do tipo efetivo do tile atual.
     */
    public String getCurrentTerrainName() {
        return currentTerrainName;
    }

    /**
     * Retorna o peso heurístico do tile atual já ajustado pelo arquétipo do agente.
     *
     * @return Custo efetivo percebido no solo corrente.
     */
    public int getCurrentTerrainCost() {
        return currentTerrainCost;
    }

    /**
     * Retorna o tempo de processamento da última busca A* em milissegundos.
     *
     * @return Latência registrada pelo pathfinder.
     */
    public double getAstarLatencyMs() {
        return astarLatencyMs;
    }

    /**
     * Retorna quantos vértices da grade foram avaliados na última execução do A*.
     *
     * @return Número de nós expandidos.
     */
    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Retorna o custo real {@code G} acumulado do caminho planejado pelo A*.
     *
     * @return Soma dos pesos ao longo da trajetória.
     */
    public int getPathCost() {
        return pathCost;
    }

    /**
     * Retorna a estimativa heurística inicial {@code H} calculada no início da última busca.
     *
     * @return Distância de Manhattan até o alvo na última busca.
     */
    public int getHeuristicDistance() {
        return heuristicDistance;
    }

    /**
     * Retorna o número de passos restantes até alcançar o jogador na rota atual.
     *
     * @return Quantidade de waypoints pendentes ou 0 se vazia.
     */
    public int getStepsRemaining() {
        return currentPath != null ? currentPath.size() : 0;
    }

    /**
     * Retorna o acumulador de fase em radianos utilizado na animação procedural do sprite.
     *
     * @return Valor temporal contínuo de animação.
     */
    public double getAnimationTimer() {
        return animationTimer;
    }
}
