package cloud.plataformatech.fadetoblack.world;

import javafx.scene.paint.Color;

/**
 * Enumeração tipada que cataloga todos os materiais de solo e obstáculos estruturais do mundo.
 *
 * <p>Desacopla em uma única fonte de verdade três dimensões do motor de jogo:
 * <ul>
 *   <li><b>Física de movimento</b>: passabilidade booleana e multiplicador real de velocidade;</li>
 *   <li><b>Inteligência Artificial</b>: peso inteiro ({@code movementCost}) ponderado no algoritmo A*;</li>
 *   <li><b>Apresentação visual</b>: par cromático (cor base e cor de acento) para texturização procedural.</li>
 * </ul>
 * </p>
 */
public enum TerrainType {

    // --- 1. Terrenos Neutros de Custo Nominal (Peso A* = 1, Velocidade = 100%) ---
    /** Piso pavimentado padrão da masmorra subterrânea. */
    STONE_FLOOR("Pedra Regular", true, 1, 1.0, Color.web("#2b2d42"), Color.web("#3d405b")),
    /** Solo vegetal firme predominante na Floresta Corrompida. */
    GRASS("Grama da Floresta", true, 1, 1.0, Color.web("#2d6a4f"), Color.web("#40916c")),
    /** Neve compactada transitável nas planícies do Pico Nevado. */
    SNOW_FLOOR("Neve Compacta", true, 1, 1.0, Color.web("#e0e1dd"), Color.web("#ffffff")),

    // --- 2. Terrenos com Penalidade ou Modificador Físico Especial ---
    /** Lamaçal pantanoso: reduz velocidade à metade (50%) e dobra o custo no grafo A* (peso 2). */
    MUD("Lamaçal Denso", true, 2, 0.50, Color.web("#582f0e"), Color.web("#7f4f24")),
    /** Piche altamente viscoso: reduz velocidade a 25% e impõe penalidade severa (peso 4) para forçar desvios. */
    TAR("Piche Viscoso", true, 4, 0.25, Color.web("#141115"), Color.web("#262329")),
    /** Neve acumulada profunda: desacelera em 65% (multiplicador 0.35) e custa 3 unidades no A*. */
    DEEP_SNOW("Neve Profunda", true, 3, 0.35, Color.web("#b8c0c8"), Color.web("#d0d6dc")),
    /** Placa de gelo polido: amplia teto de velocidade em +35% com custo nominal 1 e ativa física inercial. */
    ICE("Gelo Escorregadio", true, 1, 1.35, Color.web("#a8dadc"), Color.web("#f1faee")),

    // --- 3. Barreiras Naturais e Arquitetônicas Permanentemente Intransponíveis ---
    /** Bloco maciço de alvenaria, rocha ou vegetação fechada utilizado em perímetros e labirintos. */
    WALL("Muralha de Pedra", false, 0, 0.0, Color.web("#1b1b1e"), Color.web("#373737")),
    /** Leito fluvial profundo atravessável apenas quando coberto por uma ponte levadiça abaixada. */
    WATER("Água Profunda", false, 0, 0.0, Color.web("#1d3557"), Color.web("#457b9d")),
    /** Fenda glacial intransponível que bloqueia passagem quando a ponte retrátil está erguida. */
    ABYSS("Abismo Sombrio", false, 0, 0.0, Color.web("#0d0d0d"), Color.web("#1a1a1a")),

    // --- 4. Estruturas Especiais e Sobreposições Dinâmicas (DynamicOverride) ---
    /** Célula marcadora de portal interdimensional transitável pelo jogador e pelos inimigos. */
    PORTAL("Portal Dimensional", true, 1, 1.0, Color.web("#7209b7"), Color.web("#f72585")),
    /** Tabuleiro temporário injetado sobre água/abismo quando uma ponte levadiça está aberta (caminhável). */
    BRIDGE_PLANK("Madeira da Ponte", true, 1, 1.0, Color.web("#8c5a3c"), Color.web("#a36b47")),
    /** Barreira física temporária injetada sobre corredores quando um portão/porta encontra-se fechado. */
    GATE_BARRIER("Grade de Aço", false, 0, 0.0, Color.web("#4a4e69"), Color.web("#6c757d"));

    private final String displayName;
    private final boolean walkable;
    private final int movementCost;
    private final double speedMultiplier;
    private final Color primaryColor;
    private final Color accentColor;

    TerrainType(String displayName, boolean walkable, int movementCost, double speedMultiplier, Color primaryColor, Color accentColor) {
        this.displayName = displayName;
        this.walkable = walkable;
        this.movementCost = movementCost;
        this.speedMultiplier = speedMultiplier;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
    }

    /**
     * Retorna o nome descritivo em português exibido nos painéis de telemetria da HUD.
     *
     * @return Nome legível do material do terreno.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Indica se entidades físicas e rotas do algoritmo A* podem atravessar este tipo de célula.
     *
     * @return {@code true} se transitável; {@code false} para obstáculos sólidos, água ou abismos.
     */
    public boolean isWalkable() {
        return walkable;
    }

    /**
     * Retorna o peso heurístico nominal considerado na soma {@code G(n)} durante a busca A*.
     *
     * @return Custo inteiro de travessia (0 para células bloqueadas, >= 1 para transitáveis).
     */
    public int getMovementCost() {
        return movementCost;
    }

    /**
     * Retorna o escalar multiplicativo aplicado à velocidade base sobre este solo.
     *
     * @return Fator real (ex.: {@code 0.25} para 25% no piche, {@code 1.35} para 135% no gelo).
     */
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Retorna a cor de preenchimento principal do tile no motor de renderização.
     *
     * @return Cor primária JavaFX.
     */
    public Color getPrimaryColor() {
        return primaryColor;
    }

    /**
     * Retorna a cor secundária utilizada para detalhes texturais procedurais no tile.
     *
     * @return Cor de acento JavaFX.
     */
    public Color getAccentColor() {
        return accentColor;
    }
}

