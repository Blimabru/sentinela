package cloud.plataformatech.fadetoblack.world;

import javafx.scene.paint.Color;

/**
 * Catálogo dos biomas jogáveis disponíveis no universo de {@code Project Fade to Black}.
 *
 * <p>Centraliza metadados descritivos exibidos na barra global e nas notificações de transição,
 * além das cores atmosféricas empregadas na limpeza do plano de fundo pelo {@code Renderer}.</p>
 */
public enum MapType {
    /** Bioma subterrâneo estruturado em corredores de pedra, poças de piche e portões mecânicos. */
    DUNGEON("Masmorra Sombria", "Catacumbas subterrâneas com piche e portões de ferro", Color.web("#100c14"), Color.web("#800e13")),
    /** Bioma pantanoso dividido por rio vertical, lamaçais densos e pontes levadiças alternadas. */
    FOREST("Floresta Corrompida", "Bosque pantanoso amaldiçoado com lama e pontes levadiças", Color.web("#081c15"), Color.web("#2d6a4f")),
    /** Bioma glacial de alto contraste físico com neve profunda, placas de gelo inercial e abismo central. */
    SNOW("Pico Nevado", "Montanhas boreais com nevascas, neve profunda e placas de gelo", Color.web("#1b263b"), Color.web("#48cae4"));

    private final String title;
    private final String description;
    private final Color ambientColor;
    private final Color themeAccent;

    MapType(String title, String description, Color ambientColor, Color themeAccent) {
        this.title = title;
        this.description = description;
        this.ambientColor = ambientColor;
        this.themeAccent = themeAccent;
    }

    /**
     * Retorna o título oficial do bioma em português para exibição na HUD superior.
     *
     * @return Nome temático do mapa.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Retorna uma descrição breve das características ambientais deste bioma.
     *
     * @return Texto descritivo do cenário.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retorna a cor de fundo atmosférica utilizada na limpeza inicial do quadro pelo {@code Renderer}.
     *
     * @return Cor ambiente do mapa.
     */
    public Color getAmbientColor() {
        return ambientColor;
    }

    /**
     * Retorna a cor temática de destaque associada ao bioma (utilizada em acentos visuais e portais).
     *
     * @return Cor de destaque JavaFX.
     */
    public Color getThemeAccent() {
        return themeAccent;
    }
}

