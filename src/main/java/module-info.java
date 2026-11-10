/**
 * Descritor do módulo JPMS (Java Platform Module System) da aplicação.
 * Declara explicitamente os módulos do JavaFX e da biblioteca padrão necessários
 * em tempo de compilação e execução quando carregados via {@code --module-path}
 * (como ocorre com o plugin {@code javafx-maven-plugin}).
 */
module cloud.plataformatech.fadetoblack {
    // Necessário para o ciclo de vida de Application/Stage e recursos de UI do JavaFX
    requires javafx.controls;

    // Necessário para o pipeline gráfico 2D (Scene, Canvas, GraphicsContext, AnimationTimer)
    requires javafx.graphics;

    // Necessário para acesso a BufferedImage e ImageIO usados na gravação de PNGs em ScreenshotUtil
    requires java.desktop;

    // Expõe o pacote raiz de entrada para que o runtime do JavaFX possa instanciar
    // reflexivamente a classe GameApplication durante a inicialização modular
    exports cloud.plataformatech.fadetoblack.app;
}
