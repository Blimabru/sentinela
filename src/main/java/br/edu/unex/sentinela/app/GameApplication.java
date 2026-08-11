package br.edu.unex.sentinela.app;

import br.edu.unex.sentinela.core.GameEngine;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;

/**
 * Classe principal responsável pela inicialização da aplicação gráfica utilizando o framework JavaFX.
 * O JavaFX exige que a classe principal herde de "Application", o que fornece a estrutura necessária
 * para abrir uma janela (Stage) e iniciar a interface gráfica do sistema.
 */
public class GameApplication extends Application {
    
    // Definição das dimensões fixas da janela do jogo (800 pixels de largura por 600 pixels de altura).
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    /**
     * O método "start" é chamado automaticamente pelo JavaFX quando o programa é executado.
     * É o ponto de partida onde os elementos visuais são construídos e agrupados.
     * 
     * @param primaryStage O palco principal fornecido pelo JavaFX, análogo à moldura da janela de um programa de computador.
     */
    @Override
    public void start(Stage primaryStage) {
        // Define o texto que aparecerá na barra de título da janela do jogo.
        primaryStage.setTitle("Operação Sentinela");

        // "Group" funciona como uma pasta raiz (contêiner) onde todos os elementos gráficos do jogo serão guardados.
        Group root = new Group();
        
        // "Scene" representa o conteúdo interno da janela (cena gráfica). Ele abriga o "Group" raiz e adota as dimensões do jogo.
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        // Associa a cena gráfica recém-criada à moldura da janela (primaryStage).
        primaryStage.setScene(scene);

        // "Canvas" é como uma tela de pintura em branco. É através dele que o jogo terá seus gráficos desenhados pixel por pixel.
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        
        // Adiciona a tela de pintura (Canvas) dentro do contêiner principal (Group).
        root.getChildren().add(canvas);

        // O "GameEngine" (Motor do Jogo) é criado recebendo a cena (para ler o teclado) e as ferramentas de desenho do Canvas (GraphicsContext2D).
        GameEngine engine = new GameEngine(scene, canvas.getGraphicsContext2D());
        
        // Inicia o funcionamento contínuo do motor do jogo, que fará as atualizações constantes na tela.
        engine.start();

        // Torna a janela final visível para o usuário na tela do computador.
        primaryStage.show();
    }

    /**
     * Método de execução principal tradicional do Java. 
     * Sua única função aqui é acionar o processo interno de inicialização gráfica do JavaFX.
     * 
     * @param args Argumentos de linha de comando.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
