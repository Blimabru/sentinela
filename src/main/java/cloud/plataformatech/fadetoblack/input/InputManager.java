package cloud.plataformatech.fadetoblack.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

/**
 * Este componente atua como um sistema sensorial, monitorando constantemente o hardware de teclado.
 * Ele cria um registro em tempo real que permite saber exatamente quais teclas estão sendo mantidas
 * pressionadas pelo usuário em qualquer instante.
 */
public class InputManager {
    
    // O "HashSet" é uma coleção de dados que armazena elementos únicos. 
    // Aqui, ele atua como uma "memória", guardando o código de cada tecla que está afundada neste momento.
    private final Set<KeyCode> activeKeys = new HashSet<>();

    /**
     * Durante a sua construção, este gerenciador se acopla à cena (a tela do jogo) para 
     * escutar os eventos elétricos ou lógicos gerados pelo teclado.
     * 
     * @param scene A cena gráfica fornecida pelo JavaFX, atuando como o alvo do monitoramento de interações.
     */
    public InputManager(Scene scene) {
        // "setOnKeyPressed" programa a cena para que, no instante exato em que qualquer tecla for apertada,
        // o código dessa tecla seja adicionado à nossa memória (activeKeys).
        scene.setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        
        // "setOnKeyReleased" programa a cena para que, no momento em que a tecla for solta,
        // seu código seja imediatamente apagado da memória.
        scene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));
    }

    /**
     * Esta função atua como um sensor de consulta para o resto do sistema. Qualquer outra classe
     * (como a entidade Player) pode perguntar a este gerenciador se uma tecla de interesse está pressionada.
     * 
     * @param code O identificador único da tecla (por exemplo, "KeyCode.W" ou "KeyCode.SPACE").
     * @return O resultado será "true" (verdadeiro) apenas se a tecla constar na memória de teclas pressionadas, 
     *         caso contrário, retornará "false" (falso).
     */
    public boolean isKeyPressed(KeyCode code) {
        return activeKeys.contains(code);
    }
}
