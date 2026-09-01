package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.app.GameApplication;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.world.GameWorld;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.Tile;
import br.edu.unex.sentinela.ai.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

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

        // Fase 2: Pinta a topografia geométrica mapeada pela classe TileMap (o piso por onde o ator caminhará).
        drawTileMap(world.getTileMap());

        // Fase 3: Inspeciona o estado matemático atual das entidades e as desenha.
        drawPlayer(world.getPlayer());
        drawEnemy(world.getEnemy());

        // Fase 4: Sobrepõe textos informativos para acompanhamento e verificação do funcionamento interno.
        drawDebugInfo(deltaTime);
    }

    /**
     * Iteração em grade bidimensional para ler cada bloco da Matriz e colori-lo adequadamente 
     * criando o fundo da fase, peça por peça, da esquerda para a direita, de cima para baixo.
     * 
     * @param map Objeto mapa abstrato contendo as lógicas estruturais numéricas.
     */
    private void drawTileMap(TileMap map) {
        for (int c = 0; c < map.getCols(); c++) {
            for (int r = 0; r < map.getRows(); r++) {
                Tile tile = map.getTile(c, r);
                
                // Converte a propriedade numérica categórica em espectros visíveis de cor.
                if (tile.getType() == 1) {
                    gc.setFill(Color.DARKGRAY); // Tipo 1: Parede
                } else if (tile.getType() == 2) {
                    gc.setFill(Color.SADDLEBROWN); // Tipo 2: Lama
                } else {
                    gc.setFill(Color.DARKGREEN); // Tipo 0: Chão transitável padrão
                }
                
                // Transforma as coordenadas abstratas da matriz (ex: coluna 3, linha 2) em
                // posições absolutas na tela multiplicando pelo tamanho do bloco.
                gc.fillRect(c * TileMap.TILE_SIZE, r * TileMap.TILE_SIZE, TileMap.TILE_SIZE, TileMap.TILE_SIZE);
                
                // Desenha uma moldura tênue para facilitar a percepção da grade durante a depuração visual.
                gc.setStroke(Color.color(0, 0, 0, 0.2));
                gc.strokeRect(c * TileMap.TILE_SIZE, r * TileMap.TILE_SIZE, TileMap.TILE_SIZE, TileMap.TILE_SIZE);
            }
        }
    }

    /**
     * Rotina encarregada de extrair as coordenadas da entidade jogador e transpô-las visualmente.
     * 
     * @param player O objeto contendo a posição invisível real no plano do sistema.
     */
    private void drawPlayer(Player player) {
        // Seleciona o pincel virtual na cor azul.
        gc.setFill(Color.BLUE);
        
        // Pinta um retângulo na exata coordenada espacial baseada nas dimensões da caixa de colisão do ator.
        gc.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());
    }

    /**
     * Extrai a lógica autônoma e revela a posição do Inimigo, assim como a trilha mental 
     * (algoritmo A*) que ele está planejando usar para chegar até o jogador.
     * 
     * @param enemy O vilão.
     */
    private void drawEnemy(Enemy enemy) {
        // Primeiro, desenha a linha guia da Rota (Recurso de Debug visual exigido na aula)
        List<Node> path = enemy.getCurrentPath();
        if (path != null && !path.isEmpty()) {
            gc.setFill(Color.YELLOW);
            for (Node node : path) {
                // Desenha bolinhas amarelas no centro de cada bloco que o inimigo planeja pisar.
                double centerX = (node.getCol() * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0) - 4;
                double centerY = (node.getRow() * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0) - 4;
                gc.fillOval(centerX, centerY, 8, 8);
            }
        }

        // Depois, desenha o inimigo em si (Um quadrado vermelho).
        gc.setFill(Color.RED);
        gc.fillRect(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());
    }

    /**
     * Escreve medidores de performance em texto simples para diagnóstico, análogo a um painel de instrumentos.
     * 
     * @param deltaTime Fração de segundo transcorrida, da qual é derivada a taxa de "Quadros por Segundo" (FPS).
     */
    private void drawDebugInfo(double deltaTime) {
        // Configura o pincel para branco e a tipografia padrão para garantir boa visibilidade.
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 14));
        
        int fps = (int) (1.0 / deltaTime);
        String fpsText = String.format("FPS: %d", fps);
        String dtText = String.format("DeltaTime: %.4f s", deltaTime);

        gc.fillText(fpsText, 10, 20);
        gc.fillText(dtText, 10, 40);
    }
}
