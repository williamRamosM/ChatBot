package gui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;

final class CharacterAvatar extends JComponent {
    private static final double MAX_STRETCH = 0.06;

    private final BufferedImage restingImage;
    private final BufferedImage speakingImage;
    private final Timer timer;
    private final MouthAnimation mouth = new MouthAnimation();
    private double intensity;
    private boolean speaking;

    CharacterAvatar(BufferedImage restingImage, BufferedImage speakingImage, DoubleSupplier speechIntensity) {
        this.restingImage = restingImage;
        this.speakingImage = speakingImage;
        timer = new Timer(30, event -> {
            double volume = speechIntensity.getAsDouble();
            double next = intensity + (volume - intensity) * 0.45;
            if (next < 0.001) {
                next = 0;
            }
            boolean nextSpeaking = speakingImage != null
                    && mouth.update(volume, TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
            if (next != intensity || nextSpeaking != speaking) {
                intensity = next;
                speaking = nextSpeaking;
                repaint();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        double scale = Math.min((double) getWidth() / restingImage.getWidth(),
                getHeight() / (restingImage.getHeight() * (1 + MAX_STRETCH)));
        BufferedImage frame = speaking ? speakingImage : restingImage;
        double frameScale = scale * Math.min((double) restingImage.getWidth() / frame.getWidth(),
                (double) restingImage.getHeight() / frame.getHeight());
        int width = (int) Math.round(frame.getWidth() * frameScale);
        int height = (int) Math.round(frame.getHeight() * frameScale * (1 + intensity * MAX_STRETCH));

        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        canvas.drawImage(frame, (getWidth() - width) / 2, getHeight() - height, width, height, null);
        canvas.dispose();
    }
}
