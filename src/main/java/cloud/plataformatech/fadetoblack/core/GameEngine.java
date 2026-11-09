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
 * Orquestra o laço de jogo (Game Loop) em sincronia com a taxa de atualização do monitor.
 * Processa as etapas cíclicas de entrada do usuário, simulação física e renderização gráfica,
 * além de responder a comandos rápidos de depuração (troca de mapa, modo de HUD e screenshots).
 */
public class GameEngine extends AnimationTimer {

    private final InputManager inputManager;
    private final GameWorld gameWorld;
    private final Renderer renderer;
    private final Canvas canvas;

    private long lastTime = 0;
    private boolean hKeyWasPressed = false;
    private boolean tKeyWasPressed = false;
    private boolean f12KeyWasPressed = false;

    public GameEngine(Scene scene, Canvas canvas) {
        this.canvas = canvas;
        this.inputManager = new InputManager(scene);
        this.gameWorld = new GameWorld();
        this.renderer = new Renderer(canvas.getGraphicsContext2D());
    }

    @Override
    public void handle(long now) {
        if (lastTime == 0) {
            lastTime = now;
            return;
        }

        double deltaTime = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;

        if (deltaTime > 0.1) {
            deltaTime = 0.1;
        }

        // Processa atalhos de depuração do teclado
        handleDebugShortcuts();

        // 1. Atualização da física e inteligência artificial
        update(deltaTime);

        // 2. Renderização do quadro visual
        render(deltaTime);
    }

    private void handleDebugShortcuts() {
        // [H] - Alterna nível de detalhes do HUD overhead
        boolean hPressed = inputManager.isKeyPressed(KeyCode.H);
        if (hPressed && !hKeyWasPressed) {
            renderer.cycleHudDetailLevel();
            gameWorld.setNotification("Modo de HUD Overhead alternado");
        }
        hKeyWasPressed = hPressed;

        // [T] - Alterna ciclicamente entre os três mapas temáticos
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

        // [F12] - Captura de tela imediata
        boolean f12Pressed = inputManager.isKeyPressed(KeyCode.F12);
        if (f12Pressed && !f12KeyWasPressed) {
            String filename = "sentinela_" + System.currentTimeMillis() + ".png";
            ScreenshotUtil.saveCanvasScreenshot(canvas, filename);
            gameWorld.setNotification("Screenshot salva: " + filename);
        }
        f12KeyWasPressed = f12Pressed;
    }

    public void update(double deltaTime) {
        gameWorld.update(deltaTime, inputManager);
    }

    public void render(double deltaTime) {
        renderer.render(gameWorld, deltaTime);
    }

    public void takeScreenshot(String filename) {
        ScreenshotUtil.saveCanvasScreenshot(canvas, filename);
    }

    public GameWorld getGameWorld() {
        return gameWorld;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public Canvas getCanvas() {
        return canvas;
    }
}
