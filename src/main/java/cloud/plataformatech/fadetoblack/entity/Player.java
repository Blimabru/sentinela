package cloud.plataformatech.fadetoblack.entity;

import cloud.plataformatech.fadetoblack.input.InputManager;
import cloud.plataformatech.fadetoblack.world.TerrainType;
import cloud.plataformatech.fadetoblack.world.Tile;
import cloud.plataformatech.fadetoblack.world.TileMap;
import javafx.scene.input.KeyCode;

/**
 * Entidade controlada pelo jogador (O Paladino).
 * Incorpora mecânicas físicas sensíveis ao terreno (resistência de lama e piche,
 * deslizamento inercial sobre superfícies de gelo) e registra métricas
 * para o painel de telemetria sobre a cabeça.
 */
public class Player {

    private double x;
    private double y;
    private final double width = 32.0;
    private final double height = 32.0;
    private final double baseSpeed = 200.0;

    // Componentes inerciais para física em superfícies escorregadias (Gelo)
    private double velocityX = 0.0;
    private double velocityY = 0.0;

    // Temporizador de imunidade para evitar loop de teletransporte imediato entre portais
    private double portalCooldownTimer = 0.0;

    // Métricas de telemetria em tempo real
    private double currentSpeed = 0.0;
    private double currentSlowPercent = 0.0;
    private String currentTerrainName = "Pedra";
    private int currentTerrainCost = 1;
    private String statusEffect = "NORMAL";

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update(double deltaTime, InputManager input, TileMap tileMap) {
        if (portalCooldownTimer > 0) {
            portalCooldownTimer -= deltaTime;
        }

        // Identifica o terreno atual
        int currentTileCol = (int) ((this.x + width / 2.0) / TileMap.TILE_SIZE);
        int currentTileRow = (int) ((this.y + height / 2.0) / TileMap.TILE_SIZE);
        Tile currentTile = tileMap.getTile(currentTileCol, currentTileRow);
        TerrainType terrainType = currentTile.getEffectiveType();

        this.currentTerrainName = terrainType.getDisplayName();
        this.currentTerrainCost = currentTile.getMovementCost();

        double speedMultiplier = currentTile.getSpeedMultiplier();
        this.currentSlowPercent = (1.0 - Math.min(1.0, speedMultiplier)) * 100.0;

        // Determina a legenda do efeito de status
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

        // Leitura de intenção direcional das teclas
        double inputX = 0.0;
        double inputY = 0.0;

        if (input.isKeyPressed(KeyCode.W) || input.isKeyPressed(KeyCode.UP)) inputY -= 1.0;
        if (input.isKeyPressed(KeyCode.S) || input.isKeyPressed(KeyCode.DOWN)) inputY += 1.0;
        if (input.isKeyPressed(KeyCode.A) || input.isKeyPressed(KeyCode.LEFT)) inputX -= 1.0;
        if (input.isKeyPressed(KeyCode.D) || input.isKeyPressed(KeyCode.RIGHT)) inputX += 1.0;

        // Normalização diagonal
        if (inputX != 0 && inputY != 0) {
            double len = Math.hypot(inputX, inputY);
            inputX /= len;
            inputY /= len;
        }

        // --- Modelo Físico: Terreno Normal vs Gelo Escorregadio ---
        if (terrainType == TerrainType.ICE) {
            // No gelo: conserva inércia com baixo atrito
            double iceFriction = 0.94;
            double iceAcceleration = 600.0 * deltaTime;

            velocityX += inputX * iceAcceleration;
            velocityY += inputY * iceAcceleration;

            velocityX *= Math.pow(iceFriction, deltaTime * 60.0);
            velocityY *= Math.pow(iceFriction, deltaTime * 60.0);

            // Limite máximo de velocidade no gelo
            double currentLen = Math.hypot(velocityX, velocityY);
            if (currentLen > targetSpeed) {
                velocityX = (velocityX / currentLen) * targetSpeed;
                velocityY = (velocityY / currentLen) * targetSpeed;
            }
        } else {
            // Terreno comum ou lamacento: resposta mecânica imediata sem deslizamento
            velocityX = inputX * targetSpeed;
            velocityY = inputY * targetSpeed;
        }

        this.currentSpeed = Math.hypot(velocityX, velocityY);

        // Movimentação com checagem de colisão desacoplada por eixos (AABB)
        double nextX = this.x + velocityX * deltaTime;
        double nextY = this.y + velocityY * deltaTime;

        // Eixo Y
        if (!checkCollision(this.x, nextY, tileMap)) {
            this.y = nextY;
        } else {
            this.velocityY = 0;
        }

        // Eixo X
        if (!checkCollision(nextX, this.y, tileMap)) {
            this.x = nextX;
        } else {
            this.velocityX = 0;
        }
    }

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

    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        this.velocityX = 0;
        this.velocityY = 0;
    }

    public boolean canTeleport() {
        return portalCooldownTimer <= 0;
    }

    public void triggerPortalCooldown() {
        this.portalCooldownTimer = 1.5; // 1.5 segundos de intervalo entre portais
    }

    // --- Getters ---

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public double getCurrentSlowPercent() {
        return currentSlowPercent;
    }

    public String getCurrentTerrainName() {
        return currentTerrainName;
    }

    public int getCurrentTerrainCost() {
        return currentTerrainCost;
    }

    public String getStatusEffect() {
        return statusEffect;
    }
}
