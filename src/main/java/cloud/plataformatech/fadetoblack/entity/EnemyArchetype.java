package cloud.plataformatech.fadetoblack.entity;

import cloud.plataformatech.fadetoblack.world.Tile;
import javafx.scene.paint.Color;

/**
 * Catálogo tipado (padrão Strategy baseado em Enumeração) que especifica os três arquétipos
 * de agentes autônomos presentes na simulação.
 *
 * <p>Cada constante centraliza parâmetros locomotores (velocidade base e cadência de replanejamento
 * do grafo), identidade cromática para renderização de rotas/HUD e funções biológicas que alteram
 * tanto a percepção de custo no algoritmo A* quanto o atrito físico real sobre cada terreno.</p>
 */
public enum EnemyArchetype {

    /** Combatente terrestre robusto: sofre atrito integral dos terrenos e acelera em combate próximo. */
    DEMON(
            "Demônio",
            "Bruiser Pesado",
            95.0,
            Color.web("#d90429"),
            Color.web("#ff4d6d"),
            0.35
    ),

    /** Batedor alado de alta velocidade: mitiga pela metade a penalidade de terrenos viscosos. */
    BAT(
            "Morcego",
            "Predador Alado",
            135.0,
            Color.web("#7b2cbf"),
            Color.web("#c77dff"),
            0.28
    ),

    /** Espectro levitante: trata terrenos difíceis com custo unitário e ignora atrito de solo. */
    GHOST(
            "Espírito",
            "Espectro Flutuante",
            105.0,
            Color.web("#00b4d8"),
            Color.web("#90e0ef"),
            0.32
    );

    // Metadados textuais para painéis de diagnóstico
    private final String displayName;
    private final String roleDescription;
    // Velocidade nominal em pixels por segundo sobre terreno neutro (custo 1)
    private final double baseSpeed;
    // Paleta visual usada no corpo do sprite, pontos nodais e bordas da HUD de telemetria
    private final Color primaryColor;
    private final Color accentColor;
    // Intervalo em segundos entre recálculos periódicos do algoritmo A* (evita picos de CPU por quadro)
    private final double recalculationDelay;

    EnemyArchetype(String displayName, String roleDescription, double baseSpeed,
                   Color primaryColor, Color accentColor, double recalculationDelay) {
        this.displayName = displayName;
        this.roleDescription = roleDescription;
        this.baseSpeed = baseSpeed;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
        this.recalculationDelay = recalculationDelay;
    }

    /**
     * Traduz o custo nominal de travessia de um {@link Tile} segundo a biologia deste arquétipo.
     * Esse valor alimenta o cálculo {@code G(n)} no A*, produzindo rotas distintas entre os três
     * agentes sobre o mesmo cenário (ex.: o Demônio contorna piche, enquanto o Espírito atravessa em linha reta).
     *
     * @param tile Célula do mapa avaliada durante a expansão de vizinhos no A*.
     * @return Peso inteiro positivo percebido por este arquétipo ao cruzar o tile.
     */
    public int modifyTerrainCost(Tile tile) {
        return switch (this) {
            // Demônio percebe a resistência plena do solo (custo integral de lama, piche e neve)
            case DEMON -> tile.getMovementCost();
            // Morcego voa rente ao solo: reduz penalidades pela metade com arredondamento superior mínimo de 1
            case BAT -> tile.getMovementCost() > 1 ? Math.max(1, (tile.getMovementCost() + 1) / 2) : 1;
            // Espírito flutua acima da superfície, avaliando qualquer piso caminhável com custo uniforme 1
            case GHOST -> 1;
        };
    }

    /**
     * Calcula o fator multiplicador aplicado à velocidade física do agente sobre o tile atual.
     * Mantém coerência comportamental entre o planejamento heurístico (A*) e a execução motora.
     *
     * @param tile Célula sob o centro de massa atual da entidade.
     * @return Fator escalar aplicado sobre a velocidade base (ex.: {@code 0.25} no piche, {@code 1.0} imune).
     */
    public double calculateSpeedFactor(Tile tile) {
        return switch (this) {
            case DEMON -> tile.getSpeedMultiplier();
            // O voo do Morcego atenua lentidões em 50% (multiplicador * 1.5), sem ultrapassar 1.0 em terrenos penalizados
            case BAT -> tile.getSpeedMultiplier() < 1.0
                    ? Math.min(1.0, tile.getSpeedMultiplier() * 1.5)
                    : tile.getSpeedMultiplier();
            // Espírito é imune a atolamento e neve profunda por levitação contínua
            case GHOST -> 1.0;
        };
    }

    /**
     * Retorna o nome de exibição em português do arquétipo para HUD e rótulos.
     *
     * @return Nome curto da criatura.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retorna a classificação tática descritiva da criatura.
     *
     * @return Descrição sintética do papel comportamental.
     */
    public String getRoleDescription() {
        return roleDescription;
    }

    /**
     * Retorna a velocidade locomotora nominal em pixels por segundo.
     *
     * @return Velocidade base antes da aplicação de terrenos ou estados especiais.
     */
    public double getBaseSpeed() {
        return baseSpeed;
    }

    /**
     * Retorna a cor primária característica utilizada no sprite e nos marcadores de rota.
     *
     * @return Cor principal JavaFX do arquétipo.
     */
    public Color getPrimaryColor() {
        return primaryColor;
    }

    /**
     * Retorna a cor de destaque utilizada nas linhas de trajetória A* e cabeçalhos de HUD.
     *
     * @return Cor secundária JavaFX do arquétipo.
     */
    public Color getAccentColor() {
        return accentColor;
    }

    /**
     * Retorna o intervalo padrão (em segundos) entre replanejamentos periódicos do caminho A*.
     * Valores escalonados distribuem a carga de pathfinding e modelam o tempo de reação da IA.
     *
     * @return Período de atualização heurística em segundos.
     */
    public double getRecalculationDelay() {
        return recalculationDelay;
    }
}

