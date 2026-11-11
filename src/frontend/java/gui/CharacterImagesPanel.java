package gui;

import models.CharacterProfile;
import services.CharacterImageService;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

final class CharacterImagesPanel extends JPanel {
    private final CharacterImageService images = new CharacterImageService();
    private final JLabel restingPreview;
    private final JLabel speakingPreview;
    private final Consumer<Boolean> onBusy;
    private Path restingPath;
    private Path speakingPath;
    private boolean editable = true;
    private boolean locked;
    private boolean busy;
    private int selection;

    CharacterImagesPanel(Consumer<Boolean> onBusy) {
        this.onBusy = onBusy;
        setLayout(null);
        restingPreview = addPreview("Imagem em repouso", 0, false);
        speakingPreview = addPreview("Imagem falando", 135, true);
        showProfile(null);
    }

    Path restingPath() {
        return restingPath;
    }

    Path speakingPath() {
        return speakingPath;
    }

    void setLocked(boolean locked) {
        this.locked = locked;
    }

    void showProfile(CharacterProfile profile) {
        int currentSelection = ++selection;
        editable = profile == null;
        restingPath = null;
        speakingPath = null;
        setPreview(restingPreview, null, editable ? "Arraste a imagem" : "Sem imagem");
        setPreview(speakingPreview, null, editable ? "Primeiro, a base" : "Sem imagem");
        if (profile == null) {
            return;
        }

        new SwingWorker<Previews, Void>() {
            @Override
            protected Previews doInBackground() {
                return new Previews(loadPreview(profile.imagePath()), loadPreview(profile.speakingImagePath()));
            }

            @Override
            protected void done() {
                if (currentSelection != selection || !isDisplayable()) {
                    return;
                }
                try {
                    Previews previews = get();
                    setPreview(restingPreview, previews.resting(), unavailableText(profile.imagePath()));
                    setPreview(speakingPreview, previews.speaking(), unavailableText(profile.speakingImagePath()));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    showError(error.getCause());
                }
            }
        }.execute();
    }

    private record Previews(ImageIcon resting, ImageIcon speaking) {
    }

    private JLabel addPreview(String title, int x, boolean speakingFrame) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Roboto", Font.PLAIN, 12));
        label.setBounds(x, 0, 130, 20);
        add(label);
        JLabel preview = new JLabel("", SwingConstants.CENTER);
        preview.setFont(new Font("Roboto", Font.PLAIN, 12));
        preview.setForeground(Color.GRAY);
        preview.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        preview.setBounds(x, 24, 110, 110);
        preview.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return canEdit() && (!speakingFrame || restingPath != null)
                        && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    List<?> files = (List<?>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.size() != 1 || !(files.getFirst() instanceof File file) || !file.isFile()) {
                        throw new IllegalArgumentException("Arraste apenas um arquivo de imagem por vez.");
                    }
                    runImageTask(() -> images.importImage(file.toPath()), imported -> {
                        if (speakingFrame) {
                            speakingPath = imported.path();
                        } else {
                            restingPath = imported.path();
                            if (speakingPath == null) {
                                setPreview(speakingPreview, null, "Arraste a imagem");
                            }
                        }
                        setPreview(preview, icon(imported.image()), "");
                    });
                    return true;
                } catch (Exception error) {
                    showError(error);
                    return false;
                }
            }
        });
        add(preview);
        return preview;
    }

    private <T> void runImageTask(Callable<T> task, Consumer<T> onSuccess) {
        busy = true;
        onBusy.accept(true);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                T result;
                try {
                    result = get();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException error) {
                    if (isDisplayable()) {
                        showError(error.getCause());
                    }
                    return;
                } finally {
                    busy = false;
                    onBusy.accept(false);
                }
                if (isDisplayable()) {
                    onSuccess.accept(result);
                }
            }
        }.execute();
    }

    private boolean canEdit() {
        return editable && !locked && !busy;
    }

    private ImageIcon loadPreview(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            return icon(images.loadImage(path));
        } catch (IOException | IllegalArgumentException error) {
            return null;
        }
    }

    private String unavailableText(String path) {
        return path == null || path.isBlank() ? "Sem imagem" : "Indisponível";
    }

    private void setPreview(JLabel preview, ImageIcon icon, String emptyText) {
        preview.setIcon(icon);
        preview.setText(icon == null ? emptyText : null);
    }

    private ImageIcon icon(BufferedImage image) {
        double scale = Math.min(106.0 / image.getWidth(), 106.0 / image.getHeight());
        return new ImageIcon(image.getScaledInstance(Math.max(1, (int) (image.getWidth() * scale)),
                Math.max(1, (int) (image.getHeight() * scale)), Image.SCALE_SMOOTH));
    }

    private void showError(Throwable error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "Erro ao processar imagem", JOptionPane.ERROR_MESSAGE);
    }
}
