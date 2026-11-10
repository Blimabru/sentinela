package cloud.plataformatech.fadetoblack.entity;

import cloud.plataformatech.fadetoblack.input.InputManager;
import cloud.plataformatech.fadetoblack.world.TerrainType;
import cloud.plataformatech.fadetoblack.world.Tile;
import cloud.plataformatech.fadetoblack.world.TileMap;
import javafx.scene.input.KeyCode;

/**
 * Entidade controlada diretamente pelo usuário (O Paladino).
 *
 * <p>Implementa física locomotora sensível à natureza do solo (atrito viscoso em lama/piche/neve,
 * conservação inercial exponencial em gelo escorregadio), resolução de colisão por eixos
 * desacoplados (AABB com deslizamento em paredes), controle de histerese para portais e exposição
 * de métricas ao painel de telemetria sobre a cabeça.</p>
 */
public class Player {

    // Posição lógica do canto superior esquerdo e caixa de colisão (32x32 dentro de tiles 40x40)
    private double x;
    private double y;
    private final double width = 32.0;
    private final double height = 32.0;
    // Velocidade nominal padrão em pixels por segundo sobre piso neutro
    private final double baseSpeed = 200.0;

    // Vetor de velocidade instantânea utilizado tanto em resposta direta quanto na inércia sobre gelo
    private double velocityX = 0.0;
    private double velocityY = 0.0;

    // Temporizador de imunidade pós-teletransporte (histerese) que impede reentrada imediata em loop de portais
    private double portalCooldownTimer = 0.0;

    // --- Métricas de Telemetria em Tempo Real ---
    private double currentSpeed = 0.0;
    private double currentSlowPercent = 0.0;
    private String currentTerrainName = "Pedra";
    private int currentTerrainCost = 1;
    private String statusEffect = "NORMAL";

    /**
     * Instancia o jogador nas coordenadas lógicas fornecidas.
     *
     * @param startX Coordenada X inicial em pixels.
     * @param startY Coordenada Y inicial em pixels.
     */
    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    /**
     * Atualiza a física do protagonista, amostrando entrada direcional, atrito do terreno e colisões AABB.
     *
     * @param deltaTime Intervalo de simulação do quadro em segundos.
     * @param input     Gerenciador de teclado consultado para intenções de movimento (WASD ou Setas).
     * @param tileMap   Mapa ativo consultado para leitura de terreno atual e validação de paredes.
     */
    public void update(double deltaTime, InputManager input, TileMap tileMap) {
        if (portalCooldownTimer > 0) {
            portalCooldownTimer -= deltaTime;
        }

        // Identifica a célula exatamente sob o centro do corpo do jogador (centro da hitbox)
        int currentTileCol = (int) ((this.x + width / 2.0) / TileMap.TILE_SIZE);
        int currentTileRow = (int) ((this.y + height / 2.0) / TileMap.TILE_SIZE);
        Tile currentTile = tileMap.getTile(currentTileCol, currentTileRow);
        TerrainType terrainType = currentTile.getEffectiveType();

        // Atualiza dados de solo consumidos pelo painel HUD overhead
        this.currentTerrainName = terrainType.getDisplayName();
        this.currentTerrainCost = currentTile.getMovementCost();

        double speedMultiplier = currentTile.getSpeedMultiplier();
        // Trava o mínimo em 1.0 no cálculo de lentidão para não exibir percentuais negativos sobre o gelo (+35%)
        this.currentSlowPercent = (1.0 - Math.min(1.0, speedMultiplier)) * 100.0;

        // Traduz o efeito mecânico do solo em etiqueta diagnóstica para o usuário
        if (terrainType == TerrainType.MUD) {
            this.statusEffect = "LENTIDÃO (-50%)";
        } else if (terrainType == TerrainType.TAR) {
            this.statusEffect = "ATOLADO (-75%)";
        } else if (terrainType == TerrainType.DEEP_SNOW) {
            this.statusEffect = "NEVE DENSA (-65%)";
        } else if (terrainType == TerrainType.ICE) {
            this.statusEffect = "DESLIZANDO (+35%)";
        } else {
            this.statusEffect = "NORMAL";
        }

        double targetSpeed = baseSpeed * speedMultiplier;

        // Compõe vetor discreto de intenção direcional suportando WASD e setas simultaneamente
        double inputX = 0.0;
        double inputY = 0.0;

        if (input.isKeyPressed(KeyCode.W) || input.isKeyPressed(KeyCode.UP)) inputY -= 1.0;
        if (input.isKeyPressed(KeyCode.S) || input.isKeyPressed(KeyCode.DOWN)) inputY += 1.0;
        if (input.isKeyPressed(KeyCode.A) || input.isKeyPressed(KeyCode.LEFT)) inputX -= 1.0;
        if (input.isKeyPressed(KeyCode.D) || input.isKeyPressed(KeyCode.RIGHT)) inputX += 1.0;

        // Normalização vetorial diagonal: sem dividir pela hipotenusa, andar na diagonal somaria
        // componentes unitários resultando em velocidade sqrt(2) (~41,4%) superior ao limite físico
        if (inputX != 0 && inputY != 0) {
            double len = Math.hypot(inputX, inputY);
            inputX /= len;
            inputY /= len;
        }

        // --- Modelo Físico Dual: Inércia Contínua (Gelo) vs. Resposta Imediata (Demais Solos) ---
        if (terrainType == TerrainType.ICE) {
            // Integra aceleração gradual e aplica amortecimento exponencial independente de FPS
            // (normalizado para base 60 Hz) de modo que o jogador deslize após soltar as teclas
            double iceFriction = 0.94;
            double iceAcceleration = 600.0 * deltaTime;

            velocityX += inputX * iceAcceleration;
            velocityY += inputY * iceAcceleration;

            velocityX *= Math.pow(iceFriction, deltaTime * 60.0);
            velocityY *= Math.pow(iceFriction, deltaTime * 60.0);

            // Saturação circular de velocidade: limita a magnitude vetorial ao teto máximo do gelo
            double currentLen = Math.hypot(velocityX, velocityY);
            if (currentLen > targetSpeed) {
                velocityX = (velocityX / currentLen) * targetSpeed;
                velocityY = (velocityY / currentLen) * targetSpeed;
            }
        } else {
            // Solos convencionais ou viscosos oferecem alto atrito: resposta cinemática imediata sem derrapagem
            velocityX = inputX * targetSpeed;
            velocityY = inputY * targetSpeed;
        }

        this.currentSpeed = Math.hypot(velocityX, velocityY);

        // --- Resolução de Colisão AABB com Eixos Desacoplados ---
        // Testar Y e X separadamente permite que o jogador deslize suavemente ao longo de uma parede
        // mesmo enquanto segura uma tecla diagonal contra o obstáculo, evitando travamentos secos
        double nextX = this.x + velocityX * deltaTime;
        double nextY = this.y + velocityY * deltaTime;

        // Valida deslocamento vertical mantendo X atual constante
        if (!checkCollision(this.x, nextY, tileMap)) {
            this.y = nextY;
        } else {
            this.velocityY = 0;
        }

        // Valida deslocamento horizontal usando o Y já resolvido no passo anterior
        if (!checkCollision(nextX, this.y, tileMap)) {
            this.x = nextX;
        } else {
            this.velocityX = 0;
        }
    }

    /**
     * Verifica colisão Axis-Aligned Bounding Box (AABB) testando os quatro vértices extremos da hitbox.
     * Subtrai {@code 0.01 px} nas bordas direita e inferior para que um alinhamento exato com a divisa
     * de um tile sólido adjacente não acuse falsa penetração por arredondamento de ponto flutuante.
     *
     * @param testX   Posição X candidata do canto superior esquerdo.
     * @param testY   Posição Y candidata do canto superior esquerdo.
     * @param tileMap Grade consultada para verificar passabilidade de cada célula tocada.
     * @return {@code true} se qualquer canto tocar um tile não caminhável; {@code false} se livre.
     */
    private boolean checkCollision(double testX, double testY, TileMap tileMap) {
        int leftTile = (int) (testX / TileMap.TILE_SIZE);
        int rightTile = (int) ((testX + width - 0.01) / TileMap.TILE_SIZE);
        int topTile = (int) (testY / TileMap.TILE_SIZE);
        int bottomTile = (int) ((testY + height - 0.01) / TileMap.TILE_SIZE);

        if (!tileMap.getTile(leftTile, topTile).isWalkable()) return true;
        if (!tileMap.getTile(rightTile, topTile).isWalkable()) return true;
        if (!tileMap.getTile(leftTile, bottomTile).isWalkable()) return true;
        if (!tileMap.getTile(rightTile, bottomTile).isWalkable()) return true;

        return false;
    }

    /**
     * Teleporta o jogador para novas coordenadas e zera o vetor de velocidade inercial acumulado,
     * impedindo que o impulso remanescente do mapa anterior cause derrapagem no destino.
     *
     * @param newX Nova coordenada horizontal em pixels.
     * @param newY Nova coordenada vertical em pixels.
     */
    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        this.velocityX = 0;
        this.velocityY = 0;
    }

    /**
     * Indica se o jogador já cumpriu o intervalo de recarga necessário para atravessar outro portal.
     *
     * @return {@code true} se o temporizador de histerese estiver zerado.
     */
    public boolean canTeleport() {
        return portalCooldownTimer <= 0;
    }

    /**
     * Ativa a janela de proteção pós-teletransporte (1,5 segundos) logo após uma troca de bioma.
     */
    public void triggerPortalCooldown() {
        this.portalCooldownTimer = 1.5;
    }

    // --- Getters ---

    /**
     * Retorna a coordenada horizontal lógica em pixels.
     *
     * @return Posição X atual do jogador.
     */
    public double getX() {
        return x;
    }

    /**
     * Retorna a coordenada vertical lógica em pixels.
     *
     * @return Posição Y atual do jogador.
     */
    public double getY() {
        return y;
    }

    /**
     * Retorna a largura da hitbox do jogador em pixels.
     *
     * @return Largura lógica (32 px).
     */
    public double getWidth() {
        return width;
    }

    /**
     * Retorna a altura da hitbox do jogador em pixels.
     *
     * @return Altura lógica (32 px).
     */
    public double getHeight() {
        return height;
    }

    /**
     * Retorna a magnitude escalar da velocidade atual do jogador em pixels por segundo.
     *
     * @return Velocidade resultante atual.
     */
    public double getCurrentSpeed() {
        return currentSpeed;
    }

    /**
     * Retorna a velocidade nominal de referência sobre terreno plano neutro.
     *
     * @return Velocidade base (200 px/s).
     */
    public double getBaseSpeed() {
        return baseSpeed;
    }

    /**
     * Retorna o percentual de penalidade de velocidade aplicado pelo terreno sob o jogador.
     *
     * @return Percentual de lentidão entre 0.0 e 100.0.
     */
    public double getCurrentSlowPercent() {
        return currentSlowPercent;
    }

    /**
     * Retorna o nome descritivo do tipo efetivo de terreno sob os pés do jogador.
     *
     * @return Rótulo do terreno atual.
     */
    public String getCurrentTerrainName() {
        return currentTerrainName;
    }

    /**
     * Retorna o custo nominal de travessia A* associado ao terreno atualmente pisado.
     *
     * @return Peso inteiro do tile atual.
     */
    public int getCurrentTerrainCost() {
        return currentTerrainCost;
    }

    /**
     * Retorna o texto indicador do efeito de status físico ativo para exibição na HUD.
     *
     * @return Rótulo descritivo (ex.: {@code "NORMAL"}, {@code "DESLIZANDO (+35%)"}).
     */
    public String getStatusEffect() {
        return statusEffect;
    }
}
