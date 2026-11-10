package cloud.plataformatech.fadetoblack.app;

import cloud.plataformatech.fadetoblack.core.GameEngine;
import cloud.plataformatech.fadetoblack.world.MapType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;

import java.util.List;

/**
 * Ponto de entrada da aplicação JavaFX para o Project Fade to Black.
 * Inicializa a janela principal, configura a cena gráfica, instancia o GameEngine
 * e suporta o modo autônomo de exportação de capturas de tela via argumento de linha de comando.
 */
public class GameApplication extends Application {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Project Fade to Black - IA & Telemetria");

        double renderScale = 1.0;
        String scaleProp = System.getProperty("render.scale");
        if (scaleProp != null && !scaleProp.isBlank()) {
            try {
                renderScale = Math.max(1.0, Double.parseDouble(scaleProp.trim()));
            } catch (NumberFormatException ignored) {
                renderScale = 1.0;
            }
        }

        int canvasWidth = (int) Math.round(WIDTH * renderScale);
        int canvasHeight = (int) Math.round(HEIGHT * renderScale);

        Group root = new Group();
        Scene scene = new Scene(root, canvasWidth, canvasHeight);
        primaryStage.setScene(scene);

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        root.getChildren().add(canvas);

        GameEngine engine = new GameEngine(scene, canvas);

        // Verifica se foi solicitado o modo de geração automatizada de capturas de tela
        List<String> rawArgs = getParameters().getRaw();
        boolean captureMode = rawArgs.contains("--capture-screenshots");

        if (captureMode) {
            System.out.println("[Automated Capture] Iniciando geração de capturas de tela do projeto...");
            generateScreenshots(engine);
            Platform.exit();
            return;
        }

        // Modo de execução normal interativo
        engine.start();
        primaryStage.show();
    }

    /**
     * Gera programmaticamente as capturas de tela de todos os biomas e mecânicas
     * para inclusão comprobatória na documentação técnica (README.md).
     */
    private void generateScreenshots(GameEngine engine) {
        // 1. Mapa da Masmorra Sombria (A* dos 3 inimigos, piche e portão)
        engine.getGameWorld().changeMap(MapType.DUNGEON, 4, 4);
        engine.getGameWorld().setNotification("Bioma 1: Masmorra Sombria - Multi-Agentes A* e Piche");
        for (int i = 0; i < 10; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("1_masmorra_sombras_astar.png");

        // 2. Mapa da Floresta Corrompida (Pontes levadiças, rio, lama e piche)
        engine.getGameWorld().changeMap(MapType.FOREST, 3, 7);
        engine.getGameWorld().setNotification("Bioma 2: Floresta Corrompida - Rio e Pontes Levadiças");
        for (int i = 0; i < 10; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("2_floresta_corrompida_pontes.png");

        // 3. Mapa do Pico Nevado (Gelo escorregadio, neve profunda e abismos)
        engine.getGameWorld().changeMap(MapType.SNOW, 4, 7);
        engine.getGameWorld().setNotification("Bioma 3: Pico Nevado - Placas de Gelo e Neve Densa");
        for (int i = 0; i < 10; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("3_pico_nevado_gelo.png");

        // 4. Demonstração de Obstáculos Dinâmicos e Portais
        engine.getGameWorld().changeMap(MapType.FOREST, 8, 4);
        engine.getGameWorld().toggleNearestObstacle();
        engine.getGameWorld().setNotification("Mecânica: Portões Mecânicos e Pontes Móveis Interativas");
        for (int i = 0; i < 5; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("4_obstaculos_dinamicos_portas_pontes.png");

        // 5. Demonstração de Telemetria e Métricas de IA sobre a cabeça
        engine.getGameWorld().changeMap(MapType.DUNGEON, 9, 8);
        engine.getGameWorld().setNotification("Telemetria: Latência A*, Nós Expandidos e Custo F=G+H");
        for (int i = 0; i < 8; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("5_telemetria_metricas_ia.png");

        System.out.println("[Automated Capture] Concluída com sucesso!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
