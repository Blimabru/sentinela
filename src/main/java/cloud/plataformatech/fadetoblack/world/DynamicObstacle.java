package cloud.plataformatech.fadetoblack.world;

/**
 * Modela um obstáculo mecânico móvel no cenário (portões de ferro, portas deslizantes e pontes levadiças).
 *
 * <p>Mantém uma máquina de estados temporizada que alterna ciclicamente entre aberto e fechado
 * (ou por acionamento manual do jogador via tecla {@code E}/{@code ESPAÇO}). Desacopla a mudança
 * lógica atômica da topologia do {@link TileMap} da interpolação visual ({@code transitionProgress}),
 * permitindo que o algoritmo A* recalcule rotas imediatamente no exato quadro da transição enquanto
 * o mecanismo desliza suavemente na tela.</p>
 */
public class DynamicObstacle {

    /**
     * Classifica o comportamento estrutural e visual do obstáculo dinâmico.
     */
    public enum ObstacleType {
        /** Grade vertical metálica com barras que fecham passagens em masmorras e templos. */
        GATE("Portão de Aço"),
        /** Folha lateral deslizante que bloqueia corredores de pedra ou passagens em paliçadas. */
        SLIDING_DOOR("Porta Deslizante"),
        /** Tabuleiro retrátil posicionado sobre água ou abismo; libera caminho apenas quando aberto (abaixado). */
        DRAWBRIDGE("Ponte Levadiça");

        private final String label;

        ObstacleType(String label) {
            this.label = label;
        }

        /**
         * Retorna o nome amigável exibido nas notificações da HUD quando acionado manualmente.
         *
         * @return Rótulo descritivo do tipo de mecanismo.
         */
        public String getLabel() {
            return label;
        }
    }

    // Identificador lógico único da instância no mapa e categoria estrutural
    private final String id;
    private final ObstacleType type;
    // Âncora superior esquerda na grade e extensão retangular em tiles ocupados
    private final int col;
    private final int row;
    private final int widthTiles;
    private final int heightTiles;

    // Estado lógico discreto e temporizadores de permanência aberta/fechada (em segundos)
    private boolean open;
    private double openDuration;
    private double closedDuration;
    private double stateTimer;
    // Progresso interpolado contínuo de animação visual: 0.0 (fechado) até 1.0 (totalmente aberto)
    private double transitionProgress;
    // Flag "dirty" sinalizando ao TileMap/GameWorld que a malha navegável mudou neste quadro
    private boolean stateChangedThisFrame = false;

    /**
     * Inicializa um obstáculo mecânico configurando posição, área de cobertura e temporização cíclica.
     *
     * @param id             Identificador textual único do obstáculo.
     * @param type           Categoria estrutural ({@link ObstacleType}).
     * @param col            Coluna inicial na matriz.
     * @param row            Linha inicial na matriz.
     * @param widthTiles     Largura em quantidade de tiles.
     * @param heightTiles    Altura em quantidade de tiles.
     * @param startsOpen     {@code true} se deve iniciar aberto/transitável no carregamento do mapa.
     * @param openDuration   Duração em segundos permanecendo aberto até fechar automaticamente.
     * @param closedDuration Duração em segundos permanecendo fechado até abrir automaticamente.
     */
    public DynamicObstacle(String id, ObstacleType type, int col, int row, int widthTiles, int heightTiles,
                           boolean startsOpen, double openDuration, double closedDuration) {
        this.id = id;
        this.type = type;
        this.col = col;
        this.row = row;
        this.widthTiles = widthTiles;
        this.heightTiles = heightTiles;
        this.open = startsOpen;
        this.openDuration = openDuration;
        this.closedDuration = closedDuration;
        this.stateTimer = startsOpen ? openDuration : closedDuration;
        this.transitionProgress = startsOpen ? 1.0 : 0.0;
    }

    /**
     * Avança o temporizador cíclico e a interpolação visual deste obstáculo, alternando seu estado
     * no {@link TileMap} assim que a contagem regressiva atinge zero.
     *
     * @param deltaTime Intervalo de tempo do quadro em segundos.
     * @param tileMap   Mapa cujos tiles receberão ou perderão a sobreposição caso ocorra troca de estado.
     */
    public void update(double deltaTime, TileMap tileMap) {
        // Reinicia a sinalização do quadro anterior; só ficará true se toggle() ocorrer neste tick
        stateChangedThisFrame = false;
        stateTimer -= deltaTime;

        // Interpolação linear (ramp) para deslize visual suave a taxa de 3.0 unidades/s (~0,33s de abertura)
        double targetProgress = open ? 1.0 : 0.0;
        double speed = 3.0;
        if (transitionProgress < targetProgress) {
            transitionProgress = Math.min(targetProgress, transitionProgress + speed * deltaTime);
        } else if (transitionProgress > targetProgress) {
            transitionProgress = Math.max(targetProgress, transitionProgress - speed * deltaTime);
        }

        // Dispara alternância periódica automática ao expirar o temporizador do estado atual
        if (stateTimer <= 0) {
            toggle(tileMap);
        }
    }

    /**
     * Inverte imediatamente o estado aberto/fechado do obstáculo, reinicia a contagem regressiva,
     * sinaliza alteração topológica neste quadro e sincroniza as células correspondentes no mapa.
     *
     * @param tileMap Grade do mapa onde a passabilidade será atualizada.
     */
    public void toggle(TileMap tileMap) {
        this.open = !this.open;
        this.stateTimer = this.open ? openDuration : closedDuration;
        this.stateChangedThisFrame = true;
        applyToMap(tileMap);
    }

    /**
     * Projeta o estado lógico atual sobre todos os {@link Tile}s abrangidos por este obstáculo,
     * utilizando a camada {@code dynamicOverride} para preservar o terreno geológico subjacente.
     *
     * @param tileMap Grade do mapa onde os overrides serão aplicados ou removidos.
     */
    public void applyToMap(TileMap tileMap) {
        for (int c = col; c < col + widthTiles; c++) {
            for (int r = row; r < row + heightTiles; r++) {
                Tile tile = tileMap.getTile(c, r);
                if (tile == null) continue;

                if (type == ObstacleType.DRAWBRIDGE) {
                    // Ponte levadiça atua invertendo bloqueios naturais: aberta sobrepõe pranchas caminháveis
                    // (BRIDGE_PLANK); erguida limpa o override revelando água ou abismo intransponível
                    if (open) {
                        tile.setDynamicOverride(TerrainType.BRIDGE_PLANK);
                    } else {
                        tile.clearDynamicOverride();
                    }
                } else {
                    // Portões e portas deslizantes atuam sobre pisos caminháveis: abertos liberam o chão original;
                    // fechados injetam uma barreira sólida (GATE_BARRIER) bloqueando físicas e buscas A*
                    if (open) {
                        tile.clearDynamicOverride();
                    } else {
                        tile.setDynamicOverride(TerrainType.GATE_BARRIER);
                    }
                }
            }
        }
    }

    /**
     * Verifica se a célula {@code (testCol, testRow)} pertence à área ocupada por este obstáculo.
     *
     * @param testCol Índice de coluna consultado.
     * @param testRow Índice de linha consultado.
     * @return {@code true} se a coordenada estiver dentro do retângulo do obstáculo.
     */
    public boolean occupies(int testCol, int testRow) {
        return testCol >= col && testCol < col + widthTiles &&
               testRow >= row && testRow < row + heightTiles;
    }

    /**
     * Retorna o identificador único deste obstáculo.
     *
     * @return ID textual configurado na criação.
     */
    public String getId() {
        return id;
    }

    /**
     * Retorna a categoria funcional deste mecanismo.
     *
     * @return Tipo do obstáculo ({@link ObstacleType}).
     */
    public ObstacleType getType() {
        return type;
    }

    /**
     * Retorna a coluna âncora superior esquerda na grade.
     *
     * @return Índice inicial de coluna.
     */
    public int getCol() {
        return col;
    }

    /**
     * Retorna a linha âncora superior esquerda na grade.
     *
     * @return Índice inicial de linha.
     */
    public int getRow() {
        return row;
    }

    /**
     * Retorna a extensão horizontal do obstáculo em número de tiles.
     *
     * @return Largura em células.
     */
    public int getWidthTiles() {
        return widthTiles;
    }

    /**
     * Retorna a extensão vertical do obstáculo em número de tiles.
     *
     * @return Altura em células.
     */
    public int getHeightTiles() {
        return heightTiles;
    }

    /**
     * Informa se o obstáculo encontra-se atualmente aberto (transitável).
     *
     * @return {@code true} quando permite passagem de jogador e inimigos.
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Retorna o tempo restante em segundos até a próxima transição automática.
     *
     * @return Contagem regressiva atual exibida no badge sobre o obstáculo.
     */
    public double getStateTimer() {
        return stateTimer;
    }

    /**
     * Retorna o progresso normalizado de abertura para renderização animada ({@code 0.0} a {@code 1.0}).
     *
     * @return Fator contínuo entre fechado (0.0) e aberto (1.0).
     */
    public double getTransitionProgress() {
        return transitionProgress;
    }

    /**
     * Indica se houve mudança de estado neste tick, permitindo acionar recálculo imediato nas IAs.
     *
     * @return {@code true} exatamente no quadro em que ocorreu abertura ou fechamento.
     */
    public boolean isStateChangedThisFrame() {
        return stateChangedThisFrame;
    }
}

