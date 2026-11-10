package cloud.plataformatech.fadetoblack.app;

import cloud.plataformatech.fadetoblack.core.GameEngine;
import cloud.plataformatech.fadetoblack.world.MapType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

/**
 * Ponto de entrada da camada JavaFX para o {@code Project Fade to Black}.
 *
 * <p>Orquestra a criação da janela gráfica ({@link Stage}), a resolução lógica base vs. escala HiDPI,
 * a inicialização do {@link GameEngine} e o roteamento opcional para geração automatizada de
 * capturas de tela em lote via linha de comando ({@code --capture-screenshots}).</p>
 */
public class GameApplication extends Application {

    /**
     * Largura lógica fixa da área de jogo em pixels (equivalente a 20 colunas de 40 px).
     * Todas as regras de colisão, posicionamento de entidades e layout de HUD utilizam
     * este sistema de coordenadas lógico, independentemente da resolução física da janela.
     */
    public static final int WIDTH = 800;

    /**
     * Altura lógica fixa da área de jogo em pixels (equivalente a 15 linhas de 40 px).
     */
    public static final int HEIGHT = 600;

    /**
     * Configura e inicializa a interface gráfica principal do JavaFX.
     *
     * @param primaryStage Palco principal gerenciado pelo runtime JavaFX.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Project Fade to Black - IA & Telemetria");

        // Verifica antecipadamente se foi solicitado o modo headless/CLI de capturas
        List<String> rawArgs = getParameters().getRaw();
        boolean captureMode = rawArgs.contains("--capture-screenshots");

        // Lê a propriedade JVM opcional '-Drender.scale' (ex.: 2.00 no Docker/Xvfb HiDPI).
        // Quando omitida em execução desktop interativa (ex.: mvn javafx:run ou duplo clique),
        // calcula automaticamente a escala ideal para ocupar ~88% da área útil do monitor mantendo 4:3.
        double renderScale = 1.0;
        String scaleProp = System.getProperty("render.scale");
        if (scaleProp != null && !scaleProp.isBlank()) {
            try {
                renderScale = Math.max(1.0, Double.parseDouble(scaleProp.trim()));
            } catch (NumberFormatException ignored) {
                renderScale = 1.0;
            }
        } else if (!captureMode) {
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            double autoFitScale = Math.min((visualBounds.getWidth() * 0.88) / WIDTH,
                                           (visualBounds.getHeight() * 0.88) / HEIGHT);
            renderScale = Math.max(1.0, autoFitScale);
        }

        // Calcula as dimensões físicas iniciais do Canvas (ex.: 1600x1200 em escala 2.0x).
        // Dimensionar o backing buffer diretamente permite ao Renderer aplicar transformação afim no
        // GraphicsContext e desenhar vetores e fontes nítidos sem borramento bitmap.
        int canvasWidth = (int) Math.round(WIDTH * renderScale);
        int canvasHeight = (int) Math.round(HEIGHT * renderScale);

        // Utiliza StackPane com fundo escuro para centralizar o Canvas e habilitar redimensionamento
        // responsivo (incluindo janela maximizada) mantendo a proporção 4:3 com faixas laterais discretas
        StackPane root = new StackPane();
        root.setMinSize(0, 0);
        root.setStyle("-fx-background-color: #050505;");
        Scene scene = new Scene(root, canvasWidth, canvasHeight);
        primaryStage.setScene(scene);

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        root.getChildren().add(canvas);

        // Conecta a cena (captura de teclado) e o canvas (superfície de desenho) ao motor principal
        GameEngine engine = new GameEngine(scene, canvas);

        if (captureMode) {
            System.out.println("[Automated Capture] Iniciando geração de capturas de tela do projeto...");
            generateScreenshots(engine);
            // Encerra a thread gráfica JavaFX imediatamente após gerar os arquivos sem abrir janela
            Platform.exit();
            return;
        }

        // Redimensiona dinamicamente o buffer físico do Canvas sempre que o usuário ajustar ou maximizar
        // a janela, preservando a proporção 4:3 e garantindo renderização vetorial nítida em tempo real
        ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> {
            double sw = scene.getWidth();
            double sh = scene.getHeight();
            if (sw > 0 && sh > 0) {
                double fitScale = Math.min(sw / WIDTH, sh / HEIGHT);
                canvas.setWidth(Math.max(1.0, WIDTH * fitScale));
                canvas.setHeight(Math.max(1.0, HEIGHT * fitScale));
            }
        };
        scene.widthProperty().addListener(resizeListener);
        scene.heightProperty().addListener(resizeListener);

        // Impede colapso excessivo da janela e centraliza em execuções desktop
        primaryStage.setMinWidth(WIDTH / 2.0);
        primaryStage.setMinHeight(HEIGHT / 2.0);
        primaryStage.centerOnScreen();

        // Modo de execução normal interativo: inicia o AnimationTimer e exibe a janela
        engine.start();
        primaryStage.show();
    }

    /**
     * Simula cenários determinísticos em memória e exporta as capturas PNG de todos os biomas
     * e mecânicas para {@code docs/screenshots}, mantendo a documentação visual sincronizada com o código.
     *
     * @param engine Instância do motor utilizada para avançar ticks de simulação e desenhar o quadro.
     */
    private void generateScreenshots(GameEngine engine) {
        // 1. Mapa da Masmorra Sombria: executa 10 passos de 50 ms para que os inimigos calculem e iniciem rotas A*
        engine.getGameWorld().changeMap(MapType.DUNGEON, 4, 4);
        engine.getGameWorld().setNotification("Bioma 1: Masmorra Sombria - Multi-Agentes A* e Piche");
        for (int i = 0; i < 10; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("1_masmorra_sombras_astar.png");

        // 2. Mapa da Floresta Corrompida: demonstra rio vertical, lamaçais e pontes levadiças alternadas
        engine.getGameWorld().changeMap(MapType.FOREST, 3, 7);
        engine.getGameWorld().setNotification("Bioma 2: Floresta Corrompida - Rio e Pontes Levadiças");
        for (int i = 0; i < 10; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("2_floresta_corrompida_pontes.png");

        // 3. Mapa do Pico Nevado: posiciona o protagonista junto às placas de gelo e neve profunda
        engine.getGameWorld().changeMap(MapType.SNOW, 4, 7);
        engine.getGameWorld().setNotification("Bioma 3: Pico Nevado - Placas de Gelo e Neve Densa");
        for (int i = 0; i < 10; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("3_pico_nevado_gelo.png");

        // 4. Demonstração de Obstáculos Dinâmicos: posiciona junto à ponte norte e aciona abertura/fechamento manual
        engine.getGameWorld().changeMap(MapType.FOREST, 8, 4);
        engine.getGameWorld().toggleNearestObstacle();
        engine.getGameWorld().setNotification("Mecânica: Portões Mecânicos e Pontes Móveis Interativas");
        for (int i = 0; i < 5; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("4_obstaculos_dinamicos_portas_pontes.png");

        // 5. Demonstração de Telemetria: enquadra jogador e painéis overhead exibindo métricas de A* (F = G + H)
        engine.getGameWorld().changeMap(MapType.DUNGEON, 9, 8);
        engine.getGameWorld().setNotification("Telemetria: Latência A*, Nós Expandidos e Custo F=G+H");
        for (int i = 0; i < 8; i++) engine.update(0.05);
        engine.render(0.016);
        engine.takeScreenshot("5_telemetria_metricas_ia.png");

        System.out.println("[Automated Capture] Concluída com sucesso!");
    }

    /**
     * Inicializa o toolkit JavaFX disparando o método {@link #start(Stage)}.
     *
     * @param args Argumentos de linha de comando recebidos da JVM ou de {@link Launcher}.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
