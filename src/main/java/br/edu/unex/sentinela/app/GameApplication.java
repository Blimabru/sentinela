package br.edu.unex.sentinela.app;

import br.edu.unex.sentinela.core.GameEngine;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;

public class GameApplication extends Application {
    
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Operação Sentinela");

        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setScene(scene);

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        root.getChildren().add(canvas);

        GameEngine engine = new GameEngine(scene, canvas.getGraphicsContext2D());
        engine.start();

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
