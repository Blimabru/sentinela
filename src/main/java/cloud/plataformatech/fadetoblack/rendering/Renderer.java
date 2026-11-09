package cloud.plataformatech.fadetoblack.rendering;

import cloud.plataformatech.fadetoblack.ai.Node;
import cloud.plataformatech.fadetoblack.app.GameApplication;
import cloud.plataformatech.fadetoblack.entity.Enemy;
import cloud.plataformatech.fadetoblack.entity.Player;
import cloud.plataformatech.fadetoblack.world.DynamicObstacle;
import cloud.plataformatech.fadetoblack.world.GameWorld;
import cloud.plataformatech.fadetoblack.world.Portal;
import cloud.plataformatech.fadetoblack.world.TerrainType;
import cloud.plataformatech.fadetoblack.world.Tile;
import cloud.plataformatech.fadetoblack.world.TileMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Motor gráfico e camada de apresentação visual do Project Fade to Black.
 * Responsável por renderizar:
 * - Terrenos e biomas com texturas procedurais pixel art
 * - Obstáculos dinâmicos mecânicos com animações de abertura/fechamento
 * - Portais dimensionais com vórtices arcanos animados
 * - Trajetórias calculadas pelo algoritmo A* com waypoints coloridos por arquétipo
 * - Sprites de personagens estilizados
 * - HUDs de Telemetria flutuantes sobre a cabeça com métricas de IA e física de terreno
 * - Painel de controle superior e mensagens de status do sistema
 */
public class Renderer {

    private final GraphicsContext gc;

    // Nível de exibição das métricas overhead:
    // 0 = Completo (todas as métricas detalhadas de A*, nós e terreno)
    // 1 = Compacto (estado, latência e velocidade)
    // 2 = Oculto (apenas barras de identificação)
    private int hudDetailLevel = 0;

    public Renderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void cycleHudDetailLevel() {
        this.hudDetailLevel = (this.hudDetailLevel + 1) % 3;
    }

    public void setHudDetailLevel(int level) {
        this.hudDetailLevel = level % 3;
    }

    /**
     * Ciclo principal de renderização do quadro.
     */
    public void render(GameWorld world, double deltaTime) {
        // Limpeza de fundo com o tom atmosférico do mapa atual
        gc.setFill(world.getTileMap().getCurrentMapType().getAmbientColor());
        gc.fillRect(0, 0, GameApplication.WIDTH, GameApplication.HEIGHT);

        // 1. Renderiza a grade de terrenos
        drawTileMap(world.getTileMap());

        // 2. Renderiza os portais dimensionais
        drawPortals(world.getTileMap().getPortals());

        // 3. Renderiza os obstáculos dinâmicos (portões, portas e pontes)
        drawDynamicObstacles(world.getTileMap().getDynamicObstacles());

        // 4. Renderiza as rotas calculadas pelo A* para cada um dos 3 inimigos
        drawAStarPaths(world.getEnemies());

        // 5. Renderiza as entidades físicas
        drawPlayer(world.getPlayer());
        for (Enemy enemy : world.getEnemies()) {
            drawEnemy(enemy);
        }

        // 6. Renderiza os painéis flutuantes de Telemetria sobre a cabeça
        drawPlayerOverheadHUD(world.getPlayer());
        for (Enemy enemy : world.getEnemies()) {
            drawEnemyOverheadHUD(enemy);
        }

        // 7. Renderiza a barra de instrumentação superior e avisos globais
        drawGlobalTelemetryHeader(world, deltaTime);
    }

    // ==========================================
    // 1. RENDERIZAÇÃO DO TILEMAP E TERRENOS
    // ==========================================
    private void drawTileMap(TileMap map) {
        int tileSize = TileMap.TILE_SIZE;

        for (int c = 0; c < map.getCols(); c++) {
            for (int r = 0; r < map.getRows(); r++) {
                Tile tile = map.getTile(c, r);
                TerrainType type = tile.getEffectiveType();
                double px = c * tileSize;
                double py = r * tileSize;

                // Cor base
                gc.setFill(type.getPrimaryColor());
                gc.fillRect(px, py, tileSize, tileSize);

                // Detalhes texturais procedurais
                gc.setFill(type.getAccentColor());
                switch (type) {
                    case STONE_FLOOR -> {
                        // Linhas de rejunte dos tijolos de pedra
                        gc.setStroke(Color.web("#1e1e24", 0.4));
                        gc.strokeRect(px + 1, py + 1, tileSize - 2, tileSize - 2);
                    }
                    case GRASS -> {
                        // Folhas e tufos de grama
                        gc.fillRect(px + 8, py + 12, 3, 6);
                        gc.fillRect(px + 24, py + 20, 3, 5);
                    }
                    case MUD -> {
                        // Bolhas e poças de lama viscosa
                        gc.fillOval(px + 6, py + 8, 10, 6);
                        gc.fillOval(px + 20, py + 22, 12, 8);
                        gc.setStroke(Color.web("#3d220a", 0.5));
                        gc.strokeOval(px + 10, py + 14, 18, 12);
                    }
                    case TAR -> {
                        // Reflexos escuros de óleo e piche denso
                        gc.setFill(Color.web("#383042"));
                        gc.fillOval(px + 4, py + 6, 14, 8);
                        gc.fillOval(px + 18, py + 16, 16, 10);
                        gc.setFill(Color.web("#5a4d6b", 0.6));
                        gc.fillOval(px + 8, py + 9, 4, 3);
                    }
                    case SNOW_FLOOR -> {
                        // Sombreamento leve de neve
                        gc.setFill(Color.web("#cbd5e1", 0.4));
                        gc.fillRect(px + 2, py + tileSize - 4, tileSize - 4, 2);
                    }
                    case DEEP_SNOW -> {
                        // Textura de montículos de neve fofa acumulada
                        gc.fillOval(px + 4, py + 8, 16, 10);
                        gc.fillOval(px + 18, py + 18, 16, 12);
                    }
                    case ICE -> {
                        // Rachaduras cristalinas e brilho de gelo
                        gc.setStroke(Color.web("#ffffff", 0.7));
                        gc.strokeLine(px + 6, py + 8, px + 22, py + 24);
                        gc.strokeLine(px + 22, py + 24, px + 32, py + 20);
                    }
                    case WATER -> {
                        // Ondulações fluviais
                        gc.setStroke(Color.web("#a8dadc", 0.5));
                        gc.strokeLine(px + 4, py + 12, px + 20, py + 12);
                        gc.strokeLine(px + 18, py + 26, px + 34, py + 26);
                    }
                    case WALL -> {
                        // Bevel 3D de bloco maciço
                        gc.setFill(Color.web("#3f3f46"));
                        gc.fillRect(px + 2, py + 2, tileSize - 4, 4);
                        gc.setFill(Color.web("#18181b"));
                        gc.fillRect(px + 2, py + tileSize - 6, tileSize - 4, 4);
                    }
                    default -> {}
                }

                // Linha tênue da grade da matriz
                gc.setStroke(Color.web("#ffffff", 0.05));
                gc.strokeRect(px, py, tileSize, tileSize);
            }
        }
    }

    // ==========================================
    // 2. RENDERIZAÇÃO DOS PORTAIS
    // ==========================================
    private void drawPortals(List<Portal> portals) {
        int tileSize = TileMap.TILE_SIZE;

        for (Portal p : portals) {
            double px = p.getCol() * tileSize;
            double py = p.getRow() * tileSize;
            double pulse = p.getPulse();

            // Vórtice dimensional brilhante
            gc.setFill(Color.web("#000000", 0.6));
            gc.fillOval(px + 4, py + 4, tileSize - 8, tileSize - 8);

            Color c = p.getPortalColor();
            gc.setStroke(c);
            gc.setLineWidth(3.0 + pulse * 2.0);
            gc.strokeOval(px + 6 - pulse * 3, py + 6 - pulse * 3, tileSize - 12 + pulse * 6, tileSize - 12 + pulse * 6);

            // Núcleo do portal
            gc.setFill(Color.WHITE);
            gc.fillOval(px + tileSize / 2.0 - 4, py + tileSize / 2.0 - 4, 8, 8);

            // Rótulo do destino
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("PORTAL", px + tileSize / 2.0, py - 4);
        }
    }

    // ==========================================
    // 3. RENDERIZAÇÃO DOS OBSTÁCULOS DINÂMICOS
    // ==========================================
    private void drawDynamicObstacles(List<DynamicObstacle> obstacles) {
        int tileSize = TileMap.TILE_SIZE;

        for (DynamicObstacle obs : obstacles) {
            double px = obs.getCol() * tileSize;
            double py = obs.getRow() * tileSize;
            double w = obs.getWidthTiles() * tileSize;
            double h = obs.getHeightTiles() * tileSize;
            double progress = obs.getTransitionProgress(); // 0.0 = fechado, 1.0 = aberto

            if (obs.getType() == DynamicObstacle.ObstacleType.DRAWBRIDGE) {
                // Ponte Levadiça
                if (progress > 0.05) {
                    // Pranchas de madeira abaixadas
                    gc.setFill(Color.web("#7f4f24", progress));
                    gc.fillRect(px + 2, py + 2, w - 4, h - 4);

                    gc.setStroke(Color.web("#3d220a", progress));
                    gc.setLineWidth(2.0);
                    for (int step = 8; step < h - 4; step += 10) {
                        gc.strokeLine(px + 4, py + step, px + w - 4, py + step);
                    }
                }

                // Correntes e postes da ponte
                gc.setStroke(Color.web("#adb5bd"));
                gc.setLineWidth(1.5);
                gc.strokeLine(px, py, px, py + h);
                gc.strokeLine(px + w, py, px + w, py + h);

            } else {
                // Portão de Ferro ou Porta Deslizante
                // Fundo da moldura
                gc.setStroke(Color.web("#ced4da"));
                gc.setLineWidth(2.0);
                gc.strokeRect(px + 1, py + 1, w - 2, h - 2);

                if (progress < 0.95) {
                    // Grades ou folhas de aço fechando/fechadas
                    double slideOffset = progress * (w - 4);
                    gc.setFill(Color.web("#343a40", 1.0 - progress * 0.7));
                    gc.fillRect(px + 2 + slideOffset, py + 2, w - 4 - slideOffset, h - 4);

                    // Barras verticais do portão
                    gc.setStroke(Color.web("#ced4da", 1.0 - progress));
                    gc.setLineWidth(2.5);
                    for (double bx = px + 8 + slideOffset; bx < px + w - 4; bx += 8) {
                        gc.strokeLine(bx, py + 4, bx, py + h - 4);
                    }
                }
            }

            // Indicador de status mecânico com tempo restante
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.CENTER);
            String statusText = String.format("%s (%.1fs)", obs.isOpen() ? "ABERTO" : "FECHADO", obs.getStateTimer());
            Color badgeColor = obs.isOpen() ? Color.web("#38b000") : Color.web("#d90429");

            // Caixa do badge
            double badgeW = 68;
            double badgeH = 14;
            double bx = px + w / 2.0 - badgeW / 2.0;
            double by = py + h / 2.0 - badgeH / 2.0;

            gc.setFill(Color.web("#000000", 0.75));
            gc.fillRoundRect(bx, by, badgeW, badgeH, 4, 4);
            gc.setStroke(badgeColor);
            gc.setLineWidth(1.0);
            gc.strokeRoundRect(bx, by, badgeW, badgeH, 4, 4);

            gc.setFill(badgeColor);
            gc.fillText(statusText, px + w / 2.0, by + 10);
        }
    }

    // ==========================================
    // 4. RENDERIZAÇÃO DAS ROTAS CALCULADAS PELO A*
    // ==========================================
    private void drawAStarPaths(List<Enemy> enemies) {
        int tileSize = TileMap.TILE_SIZE;

        for (Enemy enemy : enemies) {
            List<Node> path = enemy.getCurrentPath();
            if (path == null || path.isEmpty()) continue;

            Color pathColor = enemy.getArchetype().getPrimaryColor();
            Color glowColor = enemy.getArchetype().getAccentColor();

            // 1. Linha contínua conectando os nós do caminho
            gc.setStroke(glowColor);
            gc.setLineWidth(2.5);
            gc.setLineCap(StrokeLineCap.ROUND);

            double prevX = enemy.getX() + enemy.getWidth() / 2.0;
            double prevY = enemy.getY() + enemy.getHeight() / 2.0;

            for (Node node : path) {
                double nodeCenterX = (node.getCol() * tileSize) + (tileSize / 2.0);
                double nodeCenterY = (node.getRow() * tileSize) + (tileSize / 2.0);

                gc.strokeLine(prevX, prevY, nodeCenterX, nodeCenterY);
                prevX = nodeCenterX;
                prevY = nodeCenterY;
            }

            // 2. Pontos nodais (Waypoints) com numeração ordinal de passo
            for (Node node : path) {
                double nodeCenterX = (node.getCol() * tileSize) + (tileSize / 2.0);
                double nodeCenterY = (node.getRow() * tileSize) + (tileSize / 2.0);

                // Halo brilhante
                gc.setFill(Color.web("#000000", 0.7));
                gc.fillOval(nodeCenterX - 6, nodeCenterY - 6, 12, 12);

                gc.setFill(pathColor);
                gc.fillOval(nodeCenterX - 4, nodeCenterY - 4, 8, 8);

                // Numeração do passo (quando em modo de telemetria completo)
                if (hudDetailLevel == 0 && node.getStepIndex() > 0 && node.getStepIndex() <= 9) {
                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 8));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(String.valueOf(node.getStepIndex()), nodeCenterX, nodeCenterY + 3);
                }
            }
        }
    }

    // ==========================================
    // 5. RENDERIZAÇÃO DOS PERSONAGENS
    // ==========================================
    private void drawPlayer(Player player) {
        double px = player.getX();
        double py = player.getY();
        double w = player.getWidth();
        double h = player.getHeight();

        // Aura sagrada do Paladino
        gc.setFill(Color.web("#ffd166", 0.25));
        gc.fillOval(px - 4, py - 4, w + 8, h + 8);

        // Corpo do Paladino (Armadura de aço com túnica azul)
        gc.setFill(Color.web("#1d3557"));
        gc.fillRoundRect(px, py, w, h, 6, 6);

        // Ombreiras e peitoral
        gc.setFill(Color.web("#f1faee"));
        gc.fillRect(px + 4, py + 4, w - 8, 8);

        // Emblema da Cruz Dourada
        gc.setFill(Color.web("#ffd166"));
        gc.fillRect(px + w / 2.0 - 2, py + 6, 4, 16);
        gc.fillRect(px + 6, py + 10, w - 12, 4);

        // Contorno
        gc.setStroke(Color.web("#457b9d"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(px, py, w, h, 6, 6);
    }

    private void drawEnemy(Enemy enemy) {
        double ex = enemy.getX();
        double ey = enemy.getY();
        double w = enemy.getWidth();
        double h = enemy.getHeight();
        Color pColor = enemy.getArchetype().getPrimaryColor();
        Color aColor = enemy.getArchetype().getAccentColor();
        double anim = enemy.getAnimationTimer();

        switch (enemy.getArchetype()) {
            case DEMON -> {
                // Demônio: Brute musculoso com chifres pontiagudos
                gc.setFill(pColor);
                gc.fillRoundRect(ex, ey + 4, w, h - 4, 4, 4);

                // Chifres
                gc.setFill(Color.web("#141115"));
                gc.fillPolygon(
                        new double[]{ex + 2, ex + 8, ex + 4},
                        new double[]{ey + 6, ey + 6, ey - 4},
                        3
                );
                gc.fillPolygon(
                        new double[]{ex + w - 2, ex + w - 8, ex + w - 4},
                        new double[]{ey + 6, ey + 6, ey - 4},
                        3
                );

                // Olhos flamejantes
                gc.setFill(Color.web("#ffba08"));
                gc.fillOval(ex + 8, ey + 10, 4, 4);
                gc.fillOval(ex + w - 12, ey + 10, 4, 4);
            }
            case BAT -> {
                // Morcego: Corpo alado com batimento de asas
                double wingFlap = Math.sin(anim) * 5.0;

                // Asas
                gc.setFill(pColor);
                gc.fillPolygon(
                        new double[]{ex - 6, ex + 6, ex + 6},
                        new double[]{ey + 12 - wingFlap, ey + 6, ey + 22},
                        3
                );
                gc.fillPolygon(
                        new double[]{ex + w + 6, ex + w - 6, ex + w - 6},
                        new double[]{ey + 12 - wingFlap, ey + 6, ey + 22},
                        3
                );

                // Corpo
                gc.setFill(aColor);
                gc.fillOval(ex + 4, ey + 4, w - 8, h - 8);

                // Olhos vermelhos
                gc.setFill(Color.RED);
                gc.fillOval(ex + 10, ey + 10, 3, 3);
                gc.fillOval(ex + w - 13, ey + 10, 3, 3);
            }
            case GHOST -> {
                // Espírito: Espectro flutuante com cauda translúcida
                double hover = Math.sin(anim) * 3.0;

                // Aura etérea
                gc.setFill(Color.web("#90e0ef", 0.3));
                gc.fillOval(ex - 4, ey - 4 + hover, w + 8, h + 8);

                // Cabeça e manto espectral
                gc.setFill(Color.web("#00b4d8", 0.85));
                gc.fillOval(ex, ey + hover, w, h - 6);

                // Cauda esfumaçada
                gc.fillPolygon(
                        new double[]{ex + 4, ex + w - 4, ex + w / 2.0},
                        new double[]{ey + h - 8 + hover, ey + h - 8 + hover, ey + h + 6 + hover},
                        3
                );

                // Olhos vazios brilhantes
                gc.setFill(Color.WHITE);
                gc.fillOval(ex + 8, ey + 8 + hover, 4, 5);
                gc.fillOval(ex + w - 12, ey + 8 + hover, 4, 5);
            }
        }
    }

    // ==========================================
    // 6. PAINÉIS OVERHEAD DE TELEMETRIA
    // ==========================================
    private void drawEnemyOverheadHUD(Enemy enemy) {
        if (hudDetailLevel == 2) return; // Oculto

        double ex = enemy.getX();
        double ey = enemy.getY();
        double centerX = ex + enemy.getWidth() / 2.0;

        if (hudDetailLevel == 0) {
            // MODO COMPLETO: Caixa translúcida de instrumentação com 4 linhas de métricas
            double boxW = 200.0;
            double boxH = 58.0;
            double boxX = centerX - boxW / 2.0;
            
            // Se o inimigo estiver muito perto do topo (barra global tem altura 34),
            // posiciona o HUD abaixo da entidade para evitar sobreposição
            double boxY = ey - boxH - 8.0;
            if (boxY < 38.0) {
                boxY = ey + enemy.getHeight() + 8.0;
            }

            // Mantém a caixa dentro das margens visíveis da tela
            boxX = Math.max(6, Math.min(GameApplication.WIDTH - boxW - 6, boxX));
            boxY = Math.min(GameApplication.HEIGHT - boxH - 6, boxY);

            // Fundo escuro com borda colorida do arquétipo
            gc.setFill(Color.web("#0b090a", 0.90));
            gc.fillRoundRect(boxX, boxY, boxW, boxH, 6, 6);
            gc.setStroke(enemy.getArchetype().getPrimaryColor());
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(boxX, boxY, boxW, boxH, 6, 6);

            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.LEFT);

            // Linha 1: Arquétipo e Estado da IA
            gc.setFill(enemy.getArchetype().getAccentColor());
            gc.fillText(String.format("[%s] %s", enemy.getArchetype().getDisplayName(), enemy.getAiState()), boxX + 6, boxY + 13);

            // Linha 2: Telemetria de Desempenho do A*
            gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
            gc.setFill(Color.web("#e0e1dd"));
            gc.fillText(String.format("A* Lat: %.2f ms | Nós: %d | Passos: %d",
                    enemy.getAstarLatencyMs(), enemy.getNodesExplored(), enemy.getStepsRemaining()), boxX + 6, boxY + 26);

            // Linha 3: Pontuação de Perseguição (F = G + H)
            gc.fillText(String.format("Score Rota F: %d (G:%d + H:%d)",
                    enemy.getPathCost() + enemy.getHeuristicDistance(),
                    enemy.getPathCost(), enemy.getHeuristicDistance()), boxX + 6, boxY + 39);

            // Linha 4: Velocidade e Efeito do Terreno
            Color statusColor = enemy.getCurrentSlowPercent() > 0 ? Color.web("#ffb703") : Color.web("#52b788");
            gc.setFill(statusColor);
            gc.fillText(String.format("Vel: %.0f px/s | %s (Custo:%d)",
                    enemy.getCurrentSpeed(), enemy.getCurrentTerrainName(), enemy.getCurrentTerrainCost()), boxX + 6, boxY + 52);

        } else if (hudDetailLevel == 1) {
            // MODO COMPACTO: Badge minimalista de estado e latência
            double badgeW = 120.0;
            double badgeH = 20.0;
            double badgeX = centerX - badgeW / 2.0;
            double badgeY = ey - badgeH - 6.0;

            gc.setFill(Color.web("#0b090a", 0.85));
            gc.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 4, 4);
            gc.setStroke(enemy.getArchetype().getPrimaryColor());
            gc.setLineWidth(1.0);
            gc.strokeRoundRect(badgeX, badgeY, badgeW, badgeH, 4, 4);

            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFill(Color.WHITE);
            gc.fillText(String.format("%s | A*:%.2fms", enemy.getArchetype().getDisplayName(), enemy.getAstarLatencyMs()),
                    centerX, badgeY + 14);
        }
    }

    private void drawPlayerOverheadHUD(Player player) {
        if (hudDetailLevel == 2) return;

        double px = player.getX();
        double py = player.getY();
        double centerX = px + player.getWidth() / 2.0;

        double boxW = 190.0;
        double boxH = 46.0;
        double boxX = centerX - boxW / 2.0;
        double boxY = py - boxH - 8.0;
        if (boxY < 38.0) {
            boxY = py + player.getHeight() + 8.0;
        }

        boxX = Math.max(6, Math.min(GameApplication.WIDTH - boxW - 6, boxX));
        boxY = Math.min(GameApplication.HEIGHT - boxH - 6, boxY);

        gc.setFill(Color.web("#0b090a", 0.88));
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 6, 6);
        gc.setStroke(Color.web("#ffd166"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 6, 6);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.LEFT);

        // Linha 1: Identificação
        gc.setFill(Color.web("#ffd166"));
        gc.fillText("[PALADINO] - JOGADOR", boxX + 6, boxY + 13);

        // Linha 2: Terreno e Custo
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
        gc.setFill(Color.web("#e0e1dd"));
        gc.fillText(String.format("Terreno: %s (Custo:%d)",
                player.getCurrentTerrainName(), player.getCurrentTerrainCost()), boxX + 6, boxY + 26);

        // Linha 3: Velocidade e Efeito de Terreno
        Color speedColor = player.getCurrentSlowPercent() > 0 ? Color.web("#e63946") : Color.web("#52b788");
        gc.setFill(speedColor);
        gc.fillText(String.format("Vel: %.0f/%.0f px/s | %s",
                player.getCurrentSpeed(), player.getBaseSpeed(), player.getStatusEffect()), boxX + 6, boxY + 39);
    }

    // ==========================================
    // 7. BARRA SUPERIOR DE TELEMETRIA GLOBAL
    // ==========================================
    private void drawGlobalTelemetryHeader(GameWorld world, double deltaTime) {
        // Faixa de instrumentação no topo da tela
        gc.setFill(Color.web("#050505", 0.90));
        gc.fillRect(0, 0, GameApplication.WIDTH, 34);

        gc.setStroke(Color.web("#2b2d42"));
        gc.setLineWidth(1.0);
        gc.strokeLine(0, 34, GameApplication.WIDTH, 34);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.LEFT);

        // Título do Projeto e Mapa Atual
        gc.setFill(Color.web("#ffd166"));
        gc.fillText("PROJECT FADE TO BLACK", 12, 16);

        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        gc.setFill(Color.web("#a8dadc"));
        gc.fillText(String.format("MAPA: %s", world.getTileMap().getCurrentMapType().getTitle().toUpperCase()), 12, 28);

        // Métricas de Hardware e IA
        int fps = (int) (1.0 / Math.max(0.0001, deltaTime));
        gc.setFill(Color.web("#e0e1dd"));
        gc.fillText(String.format("FPS: %d | Delta: %.3fs | Agentes IA: %d/3 Ativos",
                fps, deltaTime, world.getEnemies().size()), 260, 20);

        // Atalhos de Controle e Testes
        gc.setFill(Color.web("#ced4da"));
        gc.fillText("[E] Acionar Portão | [H] HUD IA | [T] Trocar Mapa", 520, 20);

        // Mensagem de Notificação Temporária
        String notification = world.getLastNotification();
        if (notification != null) {
            double notifW = 320;
            double notifH = 22;
            double nx = (GameApplication.WIDTH - notifW) / 2.0;
            double ny = 40;

            gc.setFill(Color.web("#000000", 0.85));
            gc.fillRoundRect(nx, ny, notifW, notifH, 6, 6);
            gc.setStroke(Color.web("#48cae4"));
            gc.setLineWidth(1.0);
            gc.strokeRoundRect(nx, ny, notifW, notifH, 6, 6);

            gc.setFill(Color.web("#48cae4"));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(notification, GameApplication.WIDTH / 2.0, ny + 15);
        }
    }
}
