package cloud.plataformatech.fadetoblack.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

/**
 * Subsistemas de captura e consulta de estado de teclado por amostragem (Polling).
 *
 * <p>Em vez de acoplar regras de física diretamente aos ouvintes assíncronos de eventos
 * do sistema operacional (que sofrem com atraso inicial de repetição de tecla e cadência irregular),
 * esta classe converte eventos discretos de pressionamento e soltura do JavaFX em um registro
 * contínuo em memória consultável sincronamente pelo Game Loop.</p>
 */
public class InputManager {

    // Utiliza HashSet pela busca em tempo constante médio O(1) a cada quadro e por eliminar
    // duplicatas automaticamente caso o sistema operacional emita eventos repetidos enquanto segurada
    private final Set<KeyCode> activeKeys = new HashSet<>();

    /**
     * Registra manipuladores de eventos na cena gráfica para manter o conjunto de teclas ativas
     * sincronizado em tempo real com a entrada física do usuário.
     *
     * @param scene Cena JavaFX alvo cujo foco captura as interações de teclado.
     */
    public InputManager(Scene scene) {
        // Insere o código da tecla no conjunto imediatamente ao evento de pressionamento
        scene.setOnKeyPressed(e -> activeKeys.add(e.getCode()));

        // Remove o código da tecla no momento da soltura, encerrando o estado contínuo
        scene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));
    }

    /**
     * Consulta instantaneamente se determinada tecla encontra-se pressionada no quadro corrente.
     * Utilizado por {@code Player}, {@code GameWorld} e {@code GameEngine} sem bloquear o loop.
     *
     * @param code Constante {@link KeyCode} correspondente à tecla monitorada (ex.: {@code KeyCode.W}).
     * @return {@code true} se a tecla estiver ativa no momento da leitura; {@code false} caso contrário.
     */
    public boolean isKeyPressed(KeyCode code) {
        return activeKeys.contains(code);
    }
}
