package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.ai.AStarPathfinder;
import br.edu.unex.sentinela.ai.Node;
import br.edu.unex.sentinela.world.TileMap;

import java.util.List;

/**
 * A classe Enemy (Inimigo) implementa um ator autônomo. Ao contrário do Player, que é escravo
 * do teclado, o Inimigo depende do Pathfinder A* para "enxergar" e calcular suas próprias rotas 
 * de perseguição de forma ativa.
 */
public class Enemy {
    
    // Coordenadas absolutas de renderização.
    private double x;
    private double y;

    // Velocidade de perseguição (pixels por segundo).
    private final double speed = 100.0;
    
    // Dimensões do quadrado.
    private final double width = 32.0;
    private final double height = 32.0;

    // O "Cérebro" que calcula as trilhas, recebendo o mapa estático.
    private final AStarPathfinder pathfinder;
    
    // O diário de bordo interno do inimigo contendo os passos precisos até chegar no alvo.
    private List<Node> currentPath;
    
    // Mecanismo de estamina intelectual: O agente recalcula a rota apenas a cada 0.5 segundos,
    // em vez de 60 vezes por segundo, economizando poder massivo de processamento do computador.
    private double pathCalculationTimer = 0.0;
    private final double PATH_RECALCULATION_DELAY = 0.5;

    /**
     * O Construtor posiciona o nêmesis no mapa e acopla um "Cérebro" virgem a ele.
     */
    public Enemy(double startX, double startY, TileMap tileMap) {
        this.x = startX;
        this.y = startY;
        this.pathfinder = new AStarPathfinder(tileMap);
    }

    /**
     * Ciclo vital do Inimigo. Pensa, traça rota e persegue.
     * 
     * @param deltaTime Fração de tempo.
     * @param targetX Coordenada do jogador (Alvo).
     * @param targetY Coordenada do jogador (Alvo).
     */
    public void update(double deltaTime, double targetX, double targetY) {
        // Incrementa o relógio interno...
        pathCalculationTimer += deltaTime;

        // Caso o timer estoure o limite de meio segundo, ordena o "cérebro" a calcular um novo trajeto.
        if (pathCalculationTimer >= PATH_RECALCULATION_DELAY) {
            pathCalculationTimer = 0; // Reseta o relógio
            
            // Converte a própria posição bruta (pixels) para o formato compreendido pela IA (Índice da Grade).
            int startCol = (int) ((this.x + width / 2) / TileMap.TILE_SIZE);
            int startRow = (int) ((this.y + height / 2) / TileMap.TILE_SIZE);
            
            // Converte a posição do inimigo jurado (jogador) para o mesmo formato de matriz.
            int endCol = (int) ((targetX + width / 2) / TileMap.TILE_SIZE);
            int endRow = (int) ((targetY + height / 2) / TileMap.TILE_SIZE);
            
            // Requisita a solução matemática ao Módulo A*
            currentPath = pathfinder.findPath(startCol, startRow, endCol, endRow);
        }

        // --- Movimentação Motora (Seguindo a Trilha) ---
        if (currentPath != null && !currentPath.isEmpty()) {
            
            // Foca os olhos estritamente no primeiro passo imediato exigido pela lista
            Node nextStep = currentPath.get(0);
            
            // Converte esse nó imaginário da mente de volta para as coordenadas brutas na tela real
            double targetPixelX = (nextStep.getCol() * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE - width) / 2.0;
            double targetPixelY = (nextStep.getRow() * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE - height) / 2.0;
            
            // Calcula o vetor direcional e a hipotenusa até o alvo
            double dx = targetPixelX - this.x;
            double dy = targetPixelY - this.y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Se o agente ainda não pisou no centro do bloco exigido...
            if (distance > 2.0) {
                // Normaliza o vetor (transfere a força total de 1 para ser multiplicada pela velocidade real).
                this.x += (dx / distance) * speed * deltaTime;
                this.y += (dy / distance) * speed * deltaTime;
            } else {
                // O Agente pisou firmemente na "migalha de pão". Arrancamos ela da lista e partimos para a próxima!
                currentPath.remove(0);
            }
        }
    }

    // --- Métodos de Leitura Analítica ---
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    
    /**
     * O motor de vídeo consumirá esta informação bruta para desenhar no chão
     * as intenções do algoritmo e comprovar, visualmente, a inteligência da busca em grafos.
     * 
     * @return A lista residual da rota atual traçada na imaginação do Agente.
     */
    public List<Node> getCurrentPath() {
        return currentPath;
    }
}
