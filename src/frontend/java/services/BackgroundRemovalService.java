package services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;

public class BackgroundRemovalService {
    private final ImageBackendClient backend = new ImageBackendClient();

    public BufferedImage remove(BufferedImage image) throws IOException, InterruptedException {
        BufferedImage result = backend.process("/remove-background", image, Duration.ofMinutes(3));
        if (!result.getColorModel().hasAlpha()
                || result.getWidth() != image.getWidth() || result.getHeight() != image.getHeight()) {
            throw new IOException("O recorte retornou uma imagem com formato ou tamanho incompatível.");
        }
        return result;
    }
}
