package cloud.plataformatech.fadetoblack.world;

import javafx.scene.paint.Color;

/**
 * Identifica os biomas e mapas disponíveis no jogo, agrupando sua estética,
 * título e configurações de iluminação ambiente.
 */
public enum MapType {
    DUNGEON("Masmorra Sombria", "Catacumbas subterrâneas com piche e portões de ferro", Color.web("#100c14"), Color.web("#800e13")),
    FOREST("Floresta Corrompida", "Bosque pantanoso amaldiçoado com lama e pontes levadiças", Color.web("#081c15"), Color.web("#2d6a4f")),
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Color getAmbientColor() {
        return ambientColor;
    }

    public Color getThemeAccent() {
        return themeAccent;
    }
}

