package services;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;

public class SpeakingImageService {
    private static final int REFERENCE_SIZE = 480;
    private final ImageBackendClient backend = new ImageBackendClient();

    public BufferedImage generate(BufferedImage original) throws IOException, InterruptedException {
        double scale = (double) REFERENCE_SIZE / Math.max(original.getWidth(), original.getHeight());
        int width = Math.max(1, (int) Math.round(original.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(original.getHeight() * scale));
        int x = (REFERENCE_SIZE - width) / 2;
        int y = (REFERENCE_SIZE - height) / 2;

        BufferedImage reference = new BufferedImage(REFERENCE_SIZE, REFERENCE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = reference.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        canvas.drawImage(original, x, y, width, height, null);
        canvas.dispose();
        BufferedImage generated = backend.process("/generate-speaking-image", reference, Duration.ofSeconds(90));

        BufferedImage aligned = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D result = aligned.createGraphics();
        result.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int outputWidth = generated.getWidth();
        int outputHeight = generated.getHeight();
        result.drawImage(generated, 0, 0, width, height,
                x * outputWidth / REFERENCE_SIZE, y * outputHeight / REFERENCE_SIZE,
                (x + width) * outputWidth / REFERENCE_SIZE, (y + height) * outputHeight / REFERENCE_SIZE, null);
        result.setComposite(AlphaComposite.DstIn);
        result.drawImage(reference.getSubimage(x, y, width, height), 0, 0, null);
        result.dispose();
        return aligned;
    }

}
