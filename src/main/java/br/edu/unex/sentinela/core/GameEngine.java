package br.edu.unex.sentinela.core;

import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.rendering.Renderer;
import br.edu.unex.sentinela.world.GameWorld;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;

/**
 * Classe responsável por gerenciar o ciclo de vida e a execução ininterrupta do jogo (Game Loop).
 * Ao herdar da classe "AnimationTimer", o sistema garante que o método "handle" será invocado 
 * em sincronia com a taxa de atualização (frequência) do monitor (geralmente 60 vezes por segundo).
 */
public class GameEngine extends AnimationTimer {

    // Componente responsável por identificar quais teclas estão sendo pressionadas.
    private final InputManager inputManager;
    
    // Componente que armazena os elementos físicos e lógicos, como o jogador e, no futuro, mapas e inimigos.
    private final GameWorld gameWorld;
    
    // Componente dedicado exclusivamente a transpor as informações numéricas do jogo para gráficos na tela.
    private final Renderer renderer;
    
    // Variável utilizada para armazenar a marca de tempo (timestamp) do quadro (frame) gerado anteriormente.
    private long lastTime = 0;

    /**
     * Construtor do motor de jogo. Ele inicializa as estruturas lógicas fundamentais antes que
     * o laço de execução comece.
     * 
     * @param scene A interface onde os eventos de teclado e clique serão monitorados.
     * @param gc A ferramenta primária de pintura necessária para desenhar formas na tela.
     */
    public GameEngine(Scene scene, GraphicsContext gc) {
        this.inputManager = new InputManager(scene);
        this.gameWorld = new GameWorld();
        this.renderer = new Renderer(gc);
    }

    /**
     * Este é o coração do jogo. O método é acionado de forma repetitiva e automática pelo JavaFX.
     * Sua função é calcular a diferença de tempo percorrida, atualizar a matemática do jogo e redesenhar a tela.
     * 
     * @param now A marcação exata do tempo no instante em que o quadro está sendo gerado (em nanossegundos).
     */
    @Override
    public void handle(long now) {
        // Se for a primeira vez que o método é executado, registra-se o tempo inicial e interrompe-se a rodada,
        // pois ainda não há um tempo anterior válido para comparação.
        if (lastTime == 0) {
            lastTime = now;
            return;
        }

        // Subtrai o tempo do quadro anterior do tempo do quadro atual para descobrir quanto tempo passou.
        // A divisão por 1.000.000.000 é efetuada para converter essa medida de nanossegundos para segundos.
        double deltaTime = (now - lastTime) / 1_000_000_000.0;
        
        // Atualiza a marcação de tempo anterior com o tempo deste exato momento, preparando para a próxima repetição.
        lastTime = now;

        // Se o computador travar brevemente, o tempo de intervalo será excessivo e pode causar erros físicos
        // (como atravessar paredes). Esta regra limita esse tempo máximo a 0,1 segundo.
        if (deltaTime > 0.1) {
            deltaTime = 0.1;
        }

        // Fase 1: Atualiza regras matemáticas, físicas e entradas do teclado.
        update(deltaTime);
        
        // Fase 2: Pinta os resultados visuais gerados pela Fase 1 na tela.
        render(deltaTime);
    }

    /**
     * Processa as transformações numéricas de posição e estado do universo do jogo.
     * Nenhuma parte visual ou gráfica sofre intervenção direta nesta função.
     * 
     * @param deltaTime Variação de tempo desde o quadro passado, vital para movimentação consistente.
     */
    private void update(double deltaTime) {
        gameWorld.update(deltaTime, inputManager);
    }

    /**
     * Encaminha os elementos calculados do jogo para o renderizador, para que este pinte cada
     * entidade na exata coordenada definida após a etapa de atualização.
     * 
     * @param deltaTime Variação de tempo, fornecida aqui para cálculos estáticos ou estatísticos de depuração na tela.
     */
    private void render(double deltaTime) {
        renderer.render(gameWorld, deltaTime);
    }
}
