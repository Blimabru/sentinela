package cloud.plataformatech.fadetoblack.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utilitário responsável por capturar o estado gráfico do {@link Canvas} em tempo de execução
 * e persistir imagens PNG sem perdas no diretório de documentação ({@code docs/screenshots}).
 *
 * <p>Como o núcleo padrão do JavaFX não fornece codificador nativo de arquivos PNG, esta classe
 * realiza a ponte com o módulo {@code java.desktop} ({@link ImageIO}) convertendo os pixels
 * capturados para {@link BufferedImage}.</p>
 */
public class ScreenshotUtil {

    /**
     * Realiza o snapshot do conteúdo renderizado no Canvas e grava um arquivo PNG em disco.
     *
     * @param canvas   Superfície gráfica JavaFX cujo framebuffer será exportado na resolução atual
     *                 (incluindo escala HiDPI quando configurada via {@code render.scale}).
     * @param filename Nome relativo do arquivo de destino dentro de {@code docs/screenshots}.
     * @return Referência ao {@link File} gravado em caso de sucesso, ou {@code null} se ocorrer falha de I/O.
     */
    public static File saveCanvasScreenshot(Canvas canvas, String filename) {
        try {
            // Extrai dimensões exatas do Canvas para preservar fidelidade pixel-a-pixel
            int width = (int) canvas.getWidth();
            int height = (int) canvas.getHeight();

            // Aloca buffer intermediário do JavaFX e solicita captura síncrona dos comandos de desenho
            WritableImage fxImage = new WritableImage(width, height);
            canvas.snapshot(null, fxImage);

            // Converte pixel por pixel do espaço JavaFX para BufferedImage ARGB compatível com ImageIO,
            // evitando dependência externa da biblioteca SwingFXUtils
            PixelReader reader = fxImage.getPixelReader();
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    bufferedImage.setRGB(x, y, reader.getArgb(x, y));
                }
            }

            // Garante que o diretório de destino exista (inclusive quando montado como volume no Docker)
            File dir = new File("docs/screenshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Grava a imagem codificada em formato PNG sem compressão com perdas e retorna o descritor
            File outputFile = new File(dir, filename);
            ImageIO.write(bufferedImage, "png", outputFile);
            System.out.println("[Screenshot] Imagem salva com sucesso: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            // Registra o erro sem lançar exceção não tratada para não interromper o Game Loop
            System.err.println("[Screenshot] Erro ao gravar arquivo PNG: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

