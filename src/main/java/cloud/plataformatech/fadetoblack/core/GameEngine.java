package cloud.plataformatech.fadetoblack.core;

import cloud.plataformatech.fadetoblack.input.InputManager;
import cloud.plataformatech.fadetoblack.rendering.Renderer;
import cloud.plataformatech.fadetoblack.util.ScreenshotUtil;
import cloud.plataformatech.fadetoblack.world.GameWorld;
import cloud.plataformatech.fadetoblack.world.MapType;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

/**
 * Núcleo de execução temporal (Game Loop) da aplicação, estruturado sobre {@link AnimationTimer}.
 *
 * <p>O JavaFX invoca {@link #handle(long)} sincronizado ao VSync na Thread de Aplicação JavaFX
 * (~60 quadros por segundo). A cada ciclo, o motor mede o tempo decorrido ({@code deltaTime})
 * para tornar física e IA independentes da taxa de quadros, processa atalhos de depuração e
 * coordena sequencialmente simulação ({@code update}) e desenho ({@code render}).</p>
 */
public class GameEngine extends AnimationTimer {

    // Sensor de entrada por estado contínuo de teclas pressionadas
    private final InputManager inputManager;
    // Domínio de simulação contendo jogador, inimigos A*, obstáculos e mapas
    private final GameWorld gameWorld;
    // Camada de renderização responsável por traduzir o estado lógico em desenho 2D no Canvas
    private final Renderer renderer;
    // Superfície de renderização referenciada também para captura de screenshots
    private final Canvas canvas;

    // Registro temporal (em nanossegundos) do quadro anterior para cálculo de deltaTime
    private long lastTime = 0;

    // Travas de detecção de borda (edge-trigger) que impedem disparo contínuo por quadro
    // enquanto as teclas de atalho [H], [T] ou [F12] permanecem pressionadas
    private boolean hKeyWasPressed = false;
    private boolean tKeyWasPressed = false;
    private boolean f12KeyWasPressed = false;

    /**
     * Instancia o motor acoplando entrada, domínio de simulação e renderizador gráfico.
     *
     * @param scene  Cena ativa que fornece eventos brutos de teclado ao {@link InputManager}.
     * @param canvas Superfície 2D onde todos os quadros serão desenhados pelo {@link Renderer}.
     */
    public GameEngine(Scene scene, Canvas canvas) {
        this.canvas = canvas;
        this.inputManager = new InputManager(scene);
        this.gameWorld = new GameWorld();
        this.renderer = new Renderer(canvas.getGraphicsContext2D());
    }

    /**
     * Pulso principal do Game Loop invocado automaticamente a cada quadro pelo {@link AnimationTimer}.
     *
     * @param now Timestamp monotônico em nanossegundos fornecido pelo runtime JavaFX.
     */
    @Override
    public void handle(long now) {
        // Primeiro quadro após start(): inicializa a referência temporal sem simular movimento,
        // evitando um deltaTime absurdo calculado contra zero
        if (lastTime == 0) {
            lastTime = now;
            return;
        }

        // Converte intervalo em nanossegundos para segundos reais (1s = 10^9 ns)
        double deltaTime = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;

        // Proteção contra picos de latência (clamp): limita o passo máximo a 100 ms para impedir
        // que congelamentos de janela/SO causem saltos excessivos e travessia indevida de paredes (tunneling)
        if (deltaTime > 0.1) {
            deltaTime = 0.1;
        }

        // Processa comandos de inspeção antes de atualizar o mundo
        handleDebugShortcuts();

        // 1. Etapa Lógica: movimentação física, colisões, ciclo de obstáculos e buscas A*
        update(deltaTime);

        // 2. Etapa Visual: rasterização do quadro atualizado e dos painéis de telemetria
        render(deltaTime);
    }

    /**
     * Avalia atalhos de diagnóstico utilizando detecção de transição (borda de subida),
     * garantindo exatamente uma ação por pressionamento físico de tecla.
     */
    private void handleDebugShortcuts() {
        // [H] - Alterna ciclicamente o nível de detalhe dos painéis de telemetria overhead (0 -> 1 -> 2)
        boolean hPressed = inputManager.isKeyPressed(KeyCode.H);
        if (hPressed && !hKeyWasPressed) {
            renderer.cycleHudDetailLevel();
            gameWorld.setNotification("Modo de HUD Overhead alternado");
        }
        hKeyWasPressed = hPressed;

        // [T] - Percorre os três biomas do projeto para testes rápidos sem cruzar portais
        boolean tPressed = inputManager.isKeyPressed(KeyCode.T);
        if (tPressed && !tKeyWasPressed) {
            MapType current = gameWorld.getTileMap().getCurrentMapType();
            MapType next = switch (current) {
                case DUNGEON -> MapType.FOREST;
                case FOREST -> MapType.SNOW;
                case SNOW -> MapType.DUNGEON;
            };
            gameWorld.changeMap(next, 4, 7);
        }
        tKeyWasPressed = tPressed;

        // [F12] - Exporta instantaneamente o framebuffer atual do Canvas para arquivo PNG carimbado
        boolean f12Pressed = inputManager.isKeyPressed(KeyCode.F12);
        if (f12Pressed && !f12KeyWasPressed) {
            String filename = "sentinela_" + System.currentTimeMillis() + ".png";
            ScreenshotUtil.saveCanvasScreenshot(canvas, filename);
            gameWorld.setNotification("Screenshot salva: " + filename);
        }
        f12KeyWasPressed = f12Pressed;
    }

    /**
     * Avança a simulação lógica e física do universo de jogo em um passo {@code deltaTime}.
     * Exposto publicamente para permitir simulação determinística fora do loop em capturas automáticas.
     *
     * @param deltaTime Intervalo de simulação em segundos.
     */
    public void update(double deltaTime) {
        gameWorld.update(deltaTime, inputManager);
    }

    /**
     * Renderiza o estado atual do {@link GameWorld} sobre o {@link Canvas}.
     *
     * @param deltaTime Intervalo do último quadro em segundos, usado na estimativa de FPS da HUD.
     */
    public void render(double deltaTime) {
        renderer.render(gameWorld, deltaTime);
    }

    /**
     * Solicita a gravação imediata do conteúdo atual do Canvas em {@code docs/screenshots}.
     *
     * @param filename Nome do arquivo PNG de saída (incluindo extensão).
     */
    public void takeScreenshot(String filename) {
        ScreenshotUtil.saveCanvasScreenshot(canvas, filename);
    }

    /**
     * Retorna a instância de domínio gerenciada pelo motor.
     *
     * @return Mundo ativo do jogo.
     */
    public GameWorld getGameWorld() {
        return gameWorld;
    }

    /**
     * Retorna o renderizador gráfico acoplado ao Canvas.
     *
     * @return Instância do renderizador.
     */
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * Retorna a superfície {@link Canvas} gerenciada pelo motor.
     *
     * @return Canvas principal de renderização.
     */
    public Canvas getCanvas() {
        return canvas;
    }
}
