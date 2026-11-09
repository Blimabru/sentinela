package cloud.plataformatech.fadetoblack.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utilitário responsável por capturar o estado gráfico do Canvas em tempo de execução
 * e exportar capturas de tela em formato PNG de alta fidelidade para a pasta de documentação.
 */
public class ScreenshotUtil {

    public static File saveCanvasScreenshot(Canvas canvas, String filename) {
        try {
            int width = (int) canvas.getWidth();
            int height = (int) canvas.getHeight();

            WritableImage fxImage = new WritableImage(width, height);
            canvas.snapshot(null, fxImage);

            PixelReader reader = fxImage.getPixelReader();
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    bufferedImage.setRGB(x, y, reader.getArgb(x, y));
                }
            }

            File dir = new File("docs/screenshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File outputFile = new File(dir, filename);
            ImageIO.write(bufferedImage, "png", outputFile);
            System.out.println("[Screenshot] Imagem salva com sucesso: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            System.err.println("[Screenshot] Erro ao gravar arquivo PNG: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

