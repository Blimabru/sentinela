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
 * Motor gráfico e camada de apresentação visual do {@code Project Fade to Black}.
 *
 * <p>Implementa um pipeline de desenho baseado no <b>Algoritmo do Pintor (Z-Order em 7 camadas)</b>
 * sobre {@link GraphicsContext} vetorial do JavaFX:
 * <ol>
 *   <li>Grade de terrenos com texturização procedural (dispensa assets bitmap externos e mantém nitidez em qualquer escala HiDPI);</li>
 *   <li>Portais interdimensionais com vórtices pulsantes;</li>
 *   <li>Obstáculos mecânicos dinâmicos (portões, portas deslizantes e pontes levadiças);</li>
 *   <li>Trajetórias e waypoints numerados calculados pelo algoritmo A*;</li>
 *   <li>Sprites vetoriais estilizados do protagonista e dos três arquétipos de inimigos;</li>
 *   <li>Painéis flutuantes (HUDs overhead) com restrição espacial anti-sobreposição;</li>
 *   <li>Barra superior fixa de telemetria global e notificações temporárias.</li>
 * </ol>
 * </p>
 */
public class Renderer {

    // Contexto gráfico imediato 2D extraído do Canvas JavaFX
    private final GraphicsContext gc;

    // Estado da densidade informacional dos painéis overhead alternado pela tecla [H]:
    // 0 = Completo (todas as métricas de A*, nós, score F=G+H e física de terreno)
    // 1 = Compacto (apenas nome e latência A* em ms)
    // 2 = Oculto (desativa HUDs individuais para visualização limpa do cenário)
    private int hudDetailLevel = 0;

    /**
     * Vincula o renderizador ao contexto gráfico do Canvas principal da cena.
     *
     * @param gc Contexto gráfico 2D obtido via {@code canvas.getGraphicsContext2D()}.
     */
    public Renderer(GraphicsContext gc) {
        this.gc = gc;
    }

    /**
     * Avança ciclicamente o nível de detalhe da HUD sobre a cabeça ({@code 0 -> 1 -> 2 -> 0}).
     */
    public void cycleHudDetailLevel() {
        this.hudDetailLevel = (this.hudDetailLevel + 1) % 3;
    }

    /**
     * Define explicitamente o modo de detalhamento da HUD utilizando módulo 3 para garantir intervalo válido.
     *
     * @param level Nível desejado ({@code 0}=Completo, {@code 1}=Compacto, {@code 2}=Oculto).
     */
    public void setHudDetailLevel(int level) {
        this.hudDetailLevel = level % 3;
    }

    /**
     * Executa o pipeline completo de renderização de um quadro em ordem estrita de profundidade (fundo -> topo).
     *
     * @param world     Estado consolidado do domínio de simulação a ser desenhado.
     * @param deltaTime Intervalo do quadro em segundos utilizado no cálculo instantâneo de FPS da barra superior.
     */
    public void render(GameWorld world, double deltaTime) {
        // Injeta matriz afim de escala HiDPI proporcional ao tamanho físico real do Canvas
        // (ex.: Canvas 1600x1200 sobre espaço lógico 800x600 aplica escala 2.0x em X e Y).
        // Por que aqui no topo: resetar com setTransform impede acúmulo cumulativo de escala entre frames e permite
        // que toda a geometria vetorial e tipografia rasterizem diretamente na resolução nativa sem borramento bitmap.
        double scaleX = gc.getCanvas().getWidth() / GameApplication.WIDTH;
        double scaleY = gc.getCanvas().getHeight() / GameApplication.HEIGHT;
        gc.setTransform(scaleX, 0, 0, scaleY, 0, 0);

        // Limpa integralmente o framebuffer com a cor atmosférica do bioma antes de repintar as camadas
        gc.setFill(world.getTileMap().getCurrentMapType().getAmbientColor());
        gc.fillRect(0, 0, GameApplication.WIDTH, GameApplication.HEIGHT);

        // 1. Camada base: matriz bidimensional de solos e paredes
        drawTileMap(world.getTileMap());

        // 2. Camada de transporte: vórtices arcanos sobre os portais
        drawPortals(world.getTileMap().getPortals());

        // 3. Camada estrutural móvel: portões, portas e pontes levadiças + badges de contagem regressiva
        drawDynamicObstacles(world.getTileMap().getDynamicObstacles());

        // 4. Camada analítica de IA: caminhos A* desenhados abaixo das entidades para não ocultar sprites
        drawAStarPaths(world.getEnemies());

        // 5. Camada de agentes físicos: protagonista seguido pelos três arquétipos inimigos
        drawPlayer(world.getPlayer());
        for (Enemy enemy : world.getEnemies()) {
            drawEnemy(enemy);
        }

        // 6. Camada diagnóstica local: painéis overhead desenhados acima dos personagens para leitura legível
        drawPlayerOverheadHUD(world.getPlayer());
        for (Enemy enemy : world.getEnemies()) {
            drawEnemyOverheadHUD(enemy);
        }

        // 7. Camada de interface global (primeiro plano absoluto): cabeçalho de sistema e notificações
        drawGlobalTelemetryHeader(world, deltaTime);
    }

    // ==========================================================================
    // 1. RENDERIZAÇÃO DO TILEMAP E TEXTURIZAÇÃO PROCEDURAL
    // ==========================================================================
    /**
     * Percorre a matriz do mapa rasterizando preenchimento base, padrões texturais geométricos
     * exclusivos de cada {@link TerrainType} e uma grade sutil para evidenciar a discretização A*.
     *
     * @param map Matriz bidimensional de células do cenário ativo.
     */
    private void drawTileMap(TileMap map) {
        int tileSize = TileMap.TILE_SIZE;

        for (int c = 0; c < map.getCols(); c++) {
            for (int r = 0; r < map.getRows(); r++) {
                Tile tile = map.getTile(c, r);
                TerrainType type = tile.getEffectiveType();
                double px = c * tileSize;
                double py = r * tileSize;

                // Preenchimento sólido primário do material do tile
                gc.setFill(type.getPrimaryColor());
                gc.fillRect(px, py, tileSize, tileSize);

                // Detalhamento procedural determinístico em coordenadas locais ao tile (px, py)
                gc.setFill(type.getAccentColor());
                switch (type) {
                    case STONE_FLOOR -> {
                        // Bordas sutis simulando rejunte de lajotas de pedra na masmorra
                        gc.setStroke(Color.web("#1e1e24", 0.4));
                        gc.strokeRect(px + 1, py + 1, tileSize - 2, tileSize - 2);
                    }
                    case GRASS -> {
                        // Pequenos feixes retangulares representando tufos de vegetação
                        gc.fillRect(px + 8, py + 12, 3, 6);
                        gc.fillRect(px + 24, py + 20, 3, 5);
                    }
                    case MUD -> {
                        // Elipses concêntricas sugerindo viscosidade e poças de lama no pântano
                        gc.fillOval(px + 6, py + 8, 10, 6);
                        gc.fillOval(px + 20, py + 22, 12, 8);
                        gc.setStroke(Color.web("#3d220a", 0.5));
                        gc.strokeOval(px + 10, py + 14, 18, 12);
                    }
                    case TAR -> {
                        // Manchas oleosas escuras com ponto especular arroxeado caracterizando piche pesado
                        gc.setFill(Color.web("#383042"));
                        gc.fillOval(px + 4, py + 6, 14, 8);
                        gc.fillOval(px + 18, py + 16, 16, 10);
                        gc.setFill(Color.web("#5a4d6b", 0.6));
                        gc.fillOval(px + 8, py + 9, 4, 3);
                    }
                    case SNOW_FLOOR -> {
                        // Sombra inferior discreta dando sensação de profundidade ao solo nevado
                        gc.setFill(Color.web("#cbd5e1", 0.4));
                        gc.fillRect(px + 2, py + tileSize - 4, tileSize - 4, 2);
                    }
                    case DEEP_SNOW -> {
                        // Bancos elevados de neve fofa acumulada que justificam a penalidade de marcha
                        gc.fillOval(px + 4, py + 8, 16, 10);
                        gc.fillOval(px + 18, py + 18, 16, 12);
                    }
                    case ICE -> {
                        // Fissuras diagonais translúcidas simulando placa de gelo cristalino
                        gc.setStroke(Color.web("#ffffff", 0.7));
                        gc.strokeLine(px + 6, py + 8, px + 22, py + 24);
                        gc.strokeLine(px + 22, py + 24, px + 32, py + 20);
                    }
                    case WATER -> {
                        // Traços horizontais representando correnteza do rio vertical
                        gc.setStroke(Color.web("#a8dadc", 0.5));
                        gc.strokeLine(px + 4, py + 12, px + 20, py + 12);
                        gc.strokeLine(px + 18, py + 26, px + 34, py + 26);
                    }
                    case WALL -> {
                        // Chanfro superior claro e base escura criando relevo pseudo-3D nas muralhas
                        gc.setFill(Color.web("#3f3f46"));
                        gc.fillRect(px + 2, py + 2, tileSize - 4, 4);
                        gc.setFill(Color.web("#18181b"));
                        gc.fillRect(px + 2, py + tileSize - 6, tileSize - 4, 4);
                    }
                    default -> {}
                }

                // Grade sutil a 5% de opacidade para tornar visível a discretização em células do A*
                gc.setStroke(Color.web("#ffffff", 0.05));
                gc.strokeRect(px, py, tileSize, tileSize);
            }
        }
    }

    // ==========================================================================
    // 2. RENDERIZAÇÃO DOS PORTAIS DIMENSIONAIS
    // ==========================================================================
    /**
     * Desenha os portais interdimensionais com halo pulsante modulado por {@link Portal#getPulse()}
     * e legenda identificadora acima do vórtice.
     *
     * @param portals Lista de portais presentes no mapa atual.
     */
    private void drawPortals(List<Portal> portals) {
        int tileSize = TileMap.TILE_SIZE;

        for (Portal p : portals) {
            double px = p.getCol() * tileSize;
            double py = p.getRow() * tileSize;
            double pulse = p.getPulse();

            // Fundo escuro circular para destacar o anel luminoso em biomas claros como o Pico Nevado
            gc.setFill(Color.web("#000000", 0.6));
            gc.fillOval(px + 4, py + 4, tileSize - 8, tileSize - 8);

            // Anel externo expansivo colorido de acordo com o bioma de destino
            Color c = p.getPortalColor();
            gc.setStroke(c);
            gc.setLineWidth(3.0 + pulse * 2.0);
            gc.strokeOval(px + 6 - pulse * 3, py + 6 - pulse * 3, tileSize - 12 + pulse * 6, tileSize - 12 + pulse * 6);

            // Núcleo branco incandescente centralizado na célula
            gc.setFill(Color.WHITE);
            gc.fillOval(px + tileSize / 2.0 - 4, py + tileSize / 2.0 - 4, 8, 8);

            // Etiqueta fixa identificando o ponto de transição
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("PORTAL", px + tileSize / 2.0, py - 4);
        }
    }

    // ==========================================================================
    // 3. RENDERIZAÇÃO DOS OBSTÁCULOS DINÂMICOS
    // ==========================================================================
    /**
     * Renderiza pontes levadiças, portões de aço e portas deslizantes interpolados continuamente
     * via {@link DynamicObstacle#getTransitionProgress()}, além de um badge central com o tempo
     * restante até a próxima mudança automática de estado.
     *
     * @param obstacles Lista de obstáculos mecânicos do mapa ativo.
     */
    private void drawDynamicObstacles(List<DynamicObstacle> obstacles) {
        int tileSize = TileMap.TILE_SIZE;

        for (DynamicObstacle obs : obstacles) {
            double px = obs.getCol() * tileSize;
            double py = obs.getRow() * tileSize;
            double w = obs.getWidthTiles() * tileSize;
            double h = obs.getHeightTiles() * tileSize;
            double progress = obs.getTransitionProgress(); // 0.0 = totalmente fechado, 1.0 = totalmente aberto

            if (obs.getType() == DynamicObstacle.ObstacleType.DRAWBRIDGE) {
                // Ponte levadiça: exibe o tabuleiro de madeira com opacidade proporcional ao abaixamento
                if (progress > 0.05) {
                    gc.setFill(Color.web("#7f4f24", progress));
                    gc.fillRect(px + 2, py + 2, w - 4, h - 4);

                    // Tábuas transversais desenhadas sobre o leito da ponte
                    gc.setStroke(Color.web("#3d220a", progress));
                    gc.setLineWidth(2.0);
                    for (int step = 8; step < h - 4; step += 10) {
                        gc.strokeLine(px + 4, py + step, px + w - 4, py + step);
                    }
                }

                // Trilhos e correntes laterais permanentes que demarcam visualmente a posição mesmo quando erguida
                gc.setStroke(Color.web("#adb5bd"));
                gc.setLineWidth(1.5);
                gc.strokeLine(px, py, px, py + h);
                gc.strokeLine(px + w, py, px + w, py + h);

            } else {
                // Portões de aço e portas deslizantes: moldura fixa delimitadora da passagem
                gc.setStroke(Color.web("#ced4da"));
                gc.setLineWidth(2.0);
                gc.strokeRect(px + 1, py + 1, w - 2, h - 2);

                if (progress < 0.95) {
                    // Desloca lateralmente a folha metálica proporcionalmente ao progresso de abertura
                    double slideOffset = progress * (w - 4);
                    gc.setFill(Color.web("#343a40", 1.0 - progress * 0.7));
                    gc.fillRect(px + 2 + slideOffset, py + 2, w - 4 - slideOffset, h - 4);

                    // Gradeado vertical metálico que acompanha o movimento de recolhimento
                    gc.setStroke(Color.web("#ced4da", 1.0 - progress));
                    gc.setLineWidth(2.5);
                    for (double bx = px + 8 + slideOffset; bx < px + w - 4; bx += 8) {
                        gc.strokeLine(bx, py + 4, bx, py + h - 4);
                    }
                }
            }

            // Badge central informando estado lógico atual (ABERTO/FECHADO) e temporizador regressivo
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.CENTER);
            String statusText = String.format("%s (%.1fs)", obs.isOpen() ? "ABERTO" : "FECHADO", obs.getStateTimer());
            Color badgeColor = obs.isOpen() ? Color.web("#38b000") : Color.web("#d90429");

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

    // ==========================================================================
    // 4. RENDERIZAÇÃO DAS ROTAS HEURÍSTICAS CALCULADAS PELO A*
    // ==========================================================================
    /**
     * Desenha sobre o terreno as polilinhas contínuas ligando cada agente à sua sequência atual
     * de nós A*, destacando waypoints com a cor característica do arquétipo e numeração de passos.
     *
     * @param enemies Lista dos agentes de IA ativos cujas rotas serão exibidas.
     */
    private void drawAStarPaths(List<Enemy> enemies) {
        int tileSize = TileMap.TILE_SIZE;

        for (Enemy enemy : enemies) {
            List<Node> path = enemy.getCurrentPath();
            if (path == null || path.isEmpty()) continue;

            Color pathColor = enemy.getArchetype().getPrimaryColor();
            Color glowColor = enemy.getArchetype().getAccentColor();

            // Subcamada 1: Traço vetorial contínuo partindo do centro da entidade até o último waypoint
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

            // Subcamada 2: Marcadores circulares em cada célula da rota para inspecionar desvios por custo
            for (Node node : path) {
                double nodeCenterX = (node.getCol() * tileSize) + (tileSize / 2.0);
                double nodeCenterY = (node.getRow() * tileSize) + (tileSize / 2.0);

                // Contorno escuro para garantir contraste sobre pisos claros (como Neve Compacta)
                gc.setFill(Color.web("#000000", 0.7));
                gc.fillOval(nodeCenterX - 6, nodeCenterY - 6, 12, 12);

                gc.setFill(pathColor);
                gc.fillOval(nodeCenterX - 4, nodeCenterY - 4, 8, 8);

                // Exibe índice ordinal dos primeiros 9 passos no modo completo para legibilidade sem poluição
                if (hudDetailLevel == 0 && node.getStepIndex() > 0 && node.getStepIndex() <= 9) {
                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 8));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(String.valueOf(node.getStepIndex()), nodeCenterX, nodeCenterY + 3);
                }
            }
        }
    }

    // ==========================================================================
    // 5. RENDERIZAÇÃO VETORIAL DOS PERSONAGENS (PROTAGONISTA E ARQUÉTIPOS)
    // ==========================================================================
    /**
     * Desenha o sprite estilizado do protagonista (Paladino) com aura dourada e emblema templário.
     *
     * @param player Entidade controlada pelo jogador.
     */
    private void drawPlayer(Player player) {
        double px = player.getX();
        double py = player.getY();
        double w = player.getWidth();
        double h = player.getHeight();

        // Aura circular translúcida facilitando localização instantânea durante perseguições intensas
        gc.setFill(Color.web("#ffd166", 0.25));
        gc.fillOval(px - 4, py - 4, w + 8, h + 8);

        // Corpo base azul-marinho com cantos arredondados
        gc.setFill(Color.web("#1d3557"));
        gc.fillRoundRect(px, py, w, h, 6, 6);

        // Ombreiras prateadas e destaque de armadura frontal
        gc.setFill(Color.web("#f1faee"));
        gc.fillRect(px + 4, py + 4, w - 8, 8);

        // Emblema cruciforme dourado sobre o peitoral
        gc.setFill(Color.web("#ffd166"));
        gc.fillRect(px + w / 2.0 - 2, py + 6, 4, 16);
        gc.fillRect(px + 6, py + 10, w - 12, 4);

        // Borda nítida delimitando a silhueta da hitbox física 32x32
        gc.setStroke(Color.web("#457b9d"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(px, py, w, h, 6, 6);
    }

    /**
     * Desenha o sprite vetorial específico para cada arquétipo de inimigo, utilizando
     * {@link Enemy#getAnimationTimer()} para animações contínuas de batimento de asas e levitação.
     *
     * @param enemy Agente autônomo a ser renderizado.
     */
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
                // Silhueta robusta vermelha com chifres triangulares e olhos âmbar
                gc.setFill(pColor);
                gc.fillRoundRect(ex, ey + 4, w, h - 4, 4, 4);

                // Par de chifres poligonais projetados acima da hitbox superior
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

                // Olhos incandescentes
                gc.setFill(Color.web("#ffba08"));
                gc.fillOval(ex + 8, ey + 10, 4, 4);
                gc.fillOval(ex + w - 12, ey + 10, 4, 4);
            }
            case BAT -> {
                // Oscilação senoidal aplicada às pontas das asas para simular voo constante
                double wingFlap = Math.sin(anim) * 5.0;

                // Asas triangulares simétricas
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

                // Núcleo circular violeta e olhos rubros
                gc.setFill(aColor);
                gc.fillOval(ex + 4, ey + 4, w - 8, h - 8);

                gc.setFill(Color.RED);
                gc.fillOval(ex + 10, ey + 10, 3, 3);
                gc.fillOval(ex + w - 13, ey + 10, 3, 3);
            }
            case GHOST -> {
                // Deslocamento vertical senoidal simulando flutuação etérea sem contato com o solo
                double hover = Math.sin(anim) * 3.0;

                // Aura translúcida ciano
                gc.setFill(Color.web("#90e0ef", 0.3));
                gc.fillOval(ex - 4, ey - 4 + hover, w + 8, h + 8);

                // Corpo semitransparente e cauda triangular espectral
                gc.setFill(Color.web("#00b4d8", 0.85));
                gc.fillOval(ex, ey + hover, w, h - 6);

                gc.fillPolygon(
                        new double[]{ex + 4, ex + w - 4, ex + w / 2.0},
                        new double[]{ey + h - 8 + hover, ey + h - 8 + hover, ey + h + 6 + hover},
                        3
                );

                // Olhos espectrais brancos
                gc.setFill(Color.WHITE);
                gc.fillOval(ex + 8, ey + 8 + hover, 4, 5);
                gc.fillOval(ex + w - 12, ey + 8 + hover, 4, 5);
            }
        }
    }

    // ==========================================================================
    // 6. PAINÉIS OVERHEAD DE TELEMETRIA INDIVIDUAL
    // ==========================================================================
    /**
     * Desenha o cartão de telemetria flutuante vinculado ao inimigo conforme o modo {@link #hudDetailLevel}.
     * No modo completo, inclui detecção de colisão com a barra superior de 34 px e grampeamento de
     * margens (viewport clamping) para manter todas as métricas 100% legíveis mesmo nos cantos do mapa.
     *
     * @param enemy Entidade de IA cuja telemetria será exibida.
     */
    private void drawEnemyOverheadHUD(Enemy enemy) {
        if (hudDetailLevel == 2) return; // Modo oculto suprime completamente os painéis locais

        double ex = enemy.getX();
        double ey = enemy.getY();
        double centerX = ex + enemy.getWidth() / 2.0;

        if (hudDetailLevel == 0) {
            // MODO COMPLETO: Caixa analítica com 4 linhas (Estado, Desempenho A*, F=G+H e Física de Terreno)
            double boxW = 200.0;
            double boxH = 58.0;
            double boxX = centerX - boxW / 2.0;

            // Posicionamento inteligente: prefere acima da cabeça; porém, se invadir a zona do cabeçalho
            // global (y < 38 px), inverte automaticamente o cartão para baixo do sprite
            double boxY = ey - boxH - 8.0;
            if (boxY < 38.0) {
                boxY = ey + enemy.getHeight() + 8.0;
            }

            // Clamping rígido dentro da área lógica útil (800x600) com margem de segurança de 6 px
            boxX = Math.max(6, Math.min(GameApplication.WIDTH - boxW - 6, boxX));
            boxY = Math.min(GameApplication.HEIGHT - boxH - 6, boxY);

            // Cartão translúcido escuro com borda colorida na identidade do arquétipo
            gc.setFill(Color.web("#0b090a", 0.90));
            gc.fillRoundRect(boxX, boxY, boxW, boxH, 6, 6);
            gc.setStroke(enemy.getArchetype().getPrimaryColor());
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(boxX, boxY, boxW, boxH, 6, 6);

            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.LEFT);

            // Linha 1: Identificação do Arquétipo e Estado Tático
            gc.setFill(enemy.getArchetype().getAccentColor());
            gc.fillText(String.format("[%s] %s", enemy.getArchetype().getDisplayName(), enemy.getAiState()), boxX + 6, boxY + 13);

            // Linha 2: Latência computacional do A*, contagem de nós avaliados e waypoints restantes
            gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
            gc.setFill(Color.web("#e0e1dd"));
            gc.fillText(String.format("A* Lat: %.2f ms | Nós: %d | Passos: %d",
                    enemy.getAstarLatencyMs(), enemy.getNodesExplored(), enemy.getStepsRemaining()), boxX + 6, boxY + 26);

            // Linha 3: Decomposição matemática da função de avaliação F = G + H
            gc.fillText(String.format("Score Rota F: %d (G:%d + H:%d)",
                    enemy.getPathCost() + enemy.getHeuristicDistance(),
                    enemy.getPathCost(), enemy.getHeuristicDistance()), boxX + 6, boxY + 39);

            // Linha 4: Velocidade instantânea e custo efetivo do solo (destaca em âmbar quando desacelerado)
            Color statusColor = enemy.getCurrentSlowPercent() > 0 ? Color.web("#ffb703") : Color.web("#52b788");
            gc.setFill(statusColor);
            gc.fillText(String.format("Vel: %.0f px/s | %s (Custo:%d)",
                    enemy.getCurrentSpeed(), enemy.getCurrentTerrainName(), enemy.getCurrentTerrainCost()), boxX + 6, boxY + 52);

        } else if (hudDetailLevel == 1) {
            // MODO COMPACTO: Etiqueta resumida exibindo apenas o nome do agente e a latência do último A*
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

    /**
     * Renderiza o painel de telemetria física acima (ou abaixo, próximo ao topo) do protagonista,
     * evidenciando em tempo real o terreno pisado, seu peso e o efeito resultante na velocidade.
     *
     * @param player Entidade do jogador.
     */
    private void drawPlayerOverheadHUD(Player player) {
        if (hudDetailLevel == 2) return;

        double px = player.getX();
        double py = player.getY();
        double centerX = px + player.getWidth() / 2.0;

        double boxW = 190.0;
        double boxH = 46.0;
        double boxX = centerX - boxW / 2.0;
        // Evita sobreposição com o cabeçalho superior global invertendo para baixo da hitbox
        double boxY = py - boxH - 8.0;
        if (boxY < 38.0) {
            boxY = py + player.getHeight() + 8.0;
        }

        // Restringe horizontal e verticalmente dentro das margens visíveis da tela
        boxX = Math.max(6, Math.min(GameApplication.WIDTH - boxW - 6, boxX));
        boxY = Math.min(GameApplication.HEIGHT - boxH - 6, boxY);

        gc.setFill(Color.web("#0b090a", 0.88));
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 6, 6);
        gc.setStroke(Color.web("#ffd166"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 6, 6);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.LEFT);

        // Linha 1: Rótulo identificador do protagonista em dourado
        gc.setFill(Color.web("#ffd166"));
        gc.fillText("[PALADINO] - JOGADOR", boxX + 6, boxY + 13);

        // Linha 2: Material e peso nominal A* da célula sob o centro do jogador
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
        gc.setFill(Color.web("#e0e1dd"));
        gc.fillText(String.format("Terreno: %s (Custo:%d)",
                player.getCurrentTerrainName(), player.getCurrentTerrainCost()), boxX + 6, boxY + 26);

        // Linha 3: Comparação entre velocidade instantânea vs. nominal e etiqueta de efeito físico
        Color speedColor = player.getCurrentSlowPercent() > 0 ? Color.web("#e63946") : Color.web("#52b788");
        gc.setFill(speedColor);
        gc.fillText(String.format("Vel: %.0f/%.0f px/s | %s",
                player.getCurrentSpeed(), player.getBaseSpeed(), player.getStatusEffect()), boxX + 6, boxY + 39);
    }

    // ==========================================================================
    // 7. BARRA SUPERIOR DE TELEMETRIA GLOBAL E NOTIFICAÇÕES
    // ==========================================================================
    /**
     * Renderiza o painel fixo no topo ({@code 800 x 34 px}) com identificação do projeto e bioma,
     * taxa instantânea de quadros por segundo (FPS), passo temporal ({@code deltaTime}), atalhos de teclado
     * e o toast de notificação temporário centralizado logo abaixo do cabeçalho.
     *
     * @param world     Mundo ativo fornecendo mapa, lista de inimigos e notificação pendente.
     * @param deltaTime Intervalo do quadro em segundos (protegido contra divisão por zero).
     */
    private void drawGlobalTelemetryHeader(GameWorld world, double deltaTime) {
        // Fundo escuro semitransparente de 34 px ao longo de toda a largura superior
        gc.setFill(Color.web("#050505", 0.90));
        gc.fillRect(0, 0, GameApplication.WIDTH, 34);

        // Filete separador inferior delimitando a área de instrumentação
        gc.setStroke(Color.web("#2b2d42"));
        gc.setLineWidth(1.0);
        gc.strokeLine(0, 34, GameApplication.WIDTH, 34);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.LEFT);

        // Coluna esquerda: título do sistema e bioma em execução
        gc.setFill(Color.web("#ffd166"));
        gc.fillText("PROJECT FADE TO BLACK", 12, 16);

        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        gc.setFill(Color.web("#a8dadc"));
        gc.fillText(String.format("MAPA: %s", world.getTileMap().getCurrentMapType().getTitle().toUpperCase()), 12, 28);

        // Coluna central: FPS protegido contra divisão por zero caso deltaTime tenda a zero
        int fps = (int) (1.0 / Math.max(0.0001, deltaTime));
        gc.setFill(Color.web("#e0e1dd"));
        gc.fillText(String.format("FPS: %d | Delta: %.3fs | Agentes IA: %d/3 Ativos",
                fps, deltaTime, world.getEnemies().size()), 260, 20);

        // Coluna direita: referência rápida de comandos interativos
        gc.setFill(Color.web("#ced4da"));
        gc.fillText("[E] Acionar Portão | [H] HUD IA | [T] Trocar Mapa", 520, 20);

        // Faixa flutuante de notificação temporária exibida em transições e acionamentos manuais
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
