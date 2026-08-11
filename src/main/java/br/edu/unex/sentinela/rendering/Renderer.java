package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.app.GameApplication;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.world.GameWorld;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * A classe "Renderer" atua como a placa de vídeo ou o ilustrador digital do projeto.
 * Ela é estritamente responsável por desenhar formas, cores e textos na tela. 
 * Nenhuma regra lógica (como perda de vida ou movimentação) pode ser alterada aqui.
 */
public class Renderer {
    
    // "GraphicsContext" é o objeto que age como um conjunto de ferramentas artísticas virtuais 
    // (um pincel), fornecendo os métodos para aplicar tinta e traços no Canvas (a tela).
    private final GraphicsContext gc;

    /**
     * Ao construir o ilustrador, fornece-se a ele o pincel que ele utilizará para pintar a tela.
     * 
     * @param gc O objeto interno do JavaFX que provê os recursos de desenho bidimensional.
     */
    public Renderer(GraphicsContext gc) {
        this.gc = gc;
    }

    /**
     * Operação principal de pintura. Ela apaga completamente o quadro que foi desenhado na fração de segundo anterior 
     * e substitui por um quadro inteiramente novo com as posições atualizadas de todos os elementos.
     * 
     * @param world O universo contendo a lista de entidades e seus estados lógicos a serem visualizados.
     * @param deltaTime Fração temporal enviada pelo motor, usada aqui para demonstrar quão rápida foi essa transição.
     */
    public void render(GameWorld world, double deltaTime) {
        // Fase 1 do desenho: "Limpeza". Para evitar o efeito de borras ou rastros na tela, 
        // seleciona-se a cor preta e desenha-se um retângulo gigante do tamanho exato da janela.
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameApplication.WIDTH, GameApplication.HEIGHT);

        // Fase 2: Inspeciona o estado matemático atual da entidade "Player" e a desenha.
        drawPlayer(world.getPlayer());

        // Fase 3: Sobrepõe textos informativos para acompanhamento e verificação do funcionamento interno.
        drawDebugInfo(deltaTime);
    }

    /**
     * Rotina encarregada de extrair as coordenadas da entidade jogador e transpô-las visualmente.
     * 
     * @param player O objeto contendo a posição invisível real no plano do sistema.
     */
    private void drawPlayer(Player player) {
        // Seleciona o pincel virtual na cor azul.
        gc.setFill(Color.BLUE);
        
        // O jogador foi especificado com dimensões estáticas de 32x32 pixels. 
        // Subtrair metade (16) das posições "x" e "y" faz com que o cálculo seja centralizado,
        // em vez de começar no canto superior esquerdo da figura.
        gc.fillRect(player.getX() - 16, player.getY() - 16, 32, 32);
    }

    /**
     * Escreve medidores de performance em texto simples para diagnóstico, análogo a um painel de instrumentos.
     * 
     * @param deltaTime Fração de segundo transcorrida, da qual é derivada a taxa de "Quadros por Segundo" (FPS).
     */
    private void drawDebugInfo(double deltaTime) {
        // Configura o pincel para branco e a tipografia padrão para garantir boa visibilidade contra o fundo preto.
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 14));
        
        // Quadros por Segundo (FPS) corresponde matematicamente ao inverso de quanto dura um único quadro.
        // Dividir um segundo pelo tempo transcorrido resulta na velocidade atual de desenho da máquina.
        int fps = (int) (1.0 / deltaTime);
        
        // Monta os textos e formata os números para que se tornem legíveis por humanos.
        String fpsText = String.format("FPS: %d", fps);
        String dtText = String.format("DeltaTime: %.4f s", deltaTime);

        // Instrui a ferramenta a carimbar esses textos em cantos específicos (X=10, Y=20 e Y=40).
        gc.fillText(fpsText, 10, 20);
        gc.fillText(dtText, 10, 40);
    }
}
