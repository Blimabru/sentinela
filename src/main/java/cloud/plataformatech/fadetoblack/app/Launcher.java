package cloud.plataformatech.fadetoblack.app;

/**
 * Ponto de entrada executável (Bootstrap Launcher) para pacotes JAR autônomos (Fat JAR).
 *
 * <p><b>Por que esta classe existe:</b> Desde o Java 11, o inicializador padrão do OpenJFX
 * recusa executar diretamente uma classe principal que estenda {@code javafx.application.Application}
 * quando carregada via Classpath tradicional ({@code java -jar}), exigindo que o JavaFX esteja
 * no {@code --module-path}. Ao delegar a chamada para {@link GameApplication#main(String[])} a
 * partir de uma classe POJO separada em módulo não nomeado, o runtime ignora essa verificação
 * modular rígida e carrega as bibliotecas embutidas no JAR normalmente.</p>
 */
public class Launcher {

    /**
     * Ponto de entrada principal referenciado pelo cabeçalho {@code Main-Class} no {@code MANIFEST.MF}.
     * Encaminha integralmente os argumentos de linha de comando (como {@code --capture-screenshots})
     * para o ciclo de vida JavaFX em {@link GameApplication}.
     *
     * @param args Argumentos de inicialização repassados pela JVM.
     */
    public static void main(String[] args) {
        GameApplication.main(args);
    }
}

