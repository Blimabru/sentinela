package cloud.plataformatech.fadetoblack.app;

/**
 * Ponto de entrada executável (Bootstrap Launcher) para pacotes JAR autônomos.
 * Permite a execução direta via comando tradicional `java -jar fade-to-black.jar`
 * ou clique duplo no arquivo, contornando a validação de componentes de runtime
 * do JavaFX quando carregado a partir do Classpath.
 */
public class Launcher {

    public static void main(String[] args) {
        GameApplication.main(args);
    }
}

