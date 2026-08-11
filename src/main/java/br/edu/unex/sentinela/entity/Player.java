package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.input.InputManager;
import javafx.scene.input.KeyCode;

/**
 * Representa o protagonista ou a entidade controlada diretamente pelas ações do usuário.
 * Esta classe armazena exclusivamente os dados lógicos (como coordenadas matemáticas) 
 * e as regras que definem como esses dados devem mudar ao longo do tempo.
 */
public class Player {
    
    // Armazenam o estado posicional da entidade dentro do mundo matemático.
    // "x" representa a posição no eixo horizontal, e "y" representa no eixo vertical.
    private double x;
    private double y;
    
    // Constante que determina a velocidade de deslocamento, ajustada para operar na 
    // razão de exatos 200 pixels transcorridos por cada segundo completo.
    private final double speed = 200.0; 
    
    /**
     * Construtor executado no momento da criação da entidade. Permite que o jogador
     * surja inicialmente em um local fixo predeterminado.
     * 
     * @param startX A posição matemática inicial no eixo horizontal da tela.
     * @param startY A posição matemática inicial no eixo vertical da tela.
     */
    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    /**
     * Realiza a atualização cíclica de cálculo de posição, processando as teclas
     * capturadas e determinando a nova coordenada espacial.
     * 
     * @param deltaTime Fração de segundo transcorrida. Multiplicá-la pela velocidade
     *                  garante que o movimento independe do tempo de resposta da máquina.
     * @param input Componente gerenciador, onde é possível verificar qual tecla foi acionada.
     */
    public void update(double deltaTime, InputManager input) {
        // Multiplicar "speed" (Pixels por segundo) por "deltaTime" (Fração de segundo decorrida)
        // calcula exatamente a quantidade correta de pixels que devem ser avançados.
        
        // Eixo Vertical: Valores de "Y" menores apontam para cima da tela.
        if (input.isKeyPressed(KeyCode.W) || input.isKeyPressed(KeyCode.UP)) {
            y -= speed * deltaTime; // Reduzir o Y move a posição visualmente para o topo.
        }
        if (input.isKeyPressed(KeyCode.S) || input.isKeyPressed(KeyCode.DOWN)) {
            y += speed * deltaTime; // Somar o Y move a posição visualmente para a base.
        }
        
        // Eixo Horizontal: Valores de "X" menores apontam para o lado esquerdo da tela.
        if (input.isKeyPressed(KeyCode.A) || input.isKeyPressed(KeyCode.LEFT)) {
            x -= speed * deltaTime; // Reduzir o X move a posição em direção ao lado esquerdo.
        }
        if (input.isKeyPressed(KeyCode.D) || input.isKeyPressed(KeyCode.RIGHT)) {
            x += speed * deltaTime; // Somar o X move a posição em direção ao lado direito.
        }
    }

    /**
     * Método de acesso para leitura de coordenadas.
     * @return O valor numérico atual da posição horizontal (X).
     */
    public double getX() {
        return x;
    }

    /**
     * Método de acesso para leitura de coordenadas.
     * @return O valor numérico atual da posição vertical (Y).
     */
    public double getY() {
        return y;
    }
}
