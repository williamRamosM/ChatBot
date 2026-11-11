package gui;

import models.CharacterImage;
import models.CharacterProfile;
import services.BackgroundRemovalService;
import services.CharacterImageDraft;
import services.CharacterImageService;
import services.SpeakingImageService;

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
    private final SpeakingImageService generator = new SpeakingImageService();
    private final BackgroundRemovalService backgroundRemoval = new BackgroundRemovalService();
    private final JLabel restingPreview;
    private final JLabel speakingPreview;
    private final JButton generate = new JButton("Gerar imagem falando");
    private final JButton removeResting;
    private final JButton removeSpeaking;
    private final Consumer<Boolean> onBusy;
    private CharacterImageDraft draft;
    private CharacterImage resting;
    private CharacterImage speaking;
    private boolean editable = true;
    private boolean locked;
    private boolean busy;
    private int selection;

    CharacterImagesPanel(Consumer<Boolean> onBusy) {
        this.onBusy = onBusy;
        setLayout(null);
        restingPreview = addPreview("Imagem em repouso", 0, false);
        speakingPreview = addPreview("Imagem falando", 135, true);
        removeResting = addRemoveButton(0, false);
        removeSpeaking = addRemoveButton(135, true);
        generate.setBounds(265, 58, 195, 32);
        generate.addActionListener(event -> generateSpeakingImage());
        add(generate);
        showProfile(null);
    }

    Path restingPath() {
        return resting == null ? null : resting.path();
    }

    Path speakingPath() {
        return speaking == null ? null : speaking.path();
    }

    void setLocked(boolean locked) {
        this.locked = locked;
        updateControls();
    }

    void showProfile(CharacterProfile profile) {
        int currentSelection = ++selection;
        discardDraft();
        draft = new CharacterImageDraft();
        editable = profile == null;
        resting = null;
        speaking = null;
        setPreview(restingPreview, null, editable ? "Arraste a imagem" : "Sem imagem");
        setPreview(speakingPreview, null, editable ? "Primeiro, a base" : "Sem imagem");
        updateControls();
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

    void discardDraft() {
        if (draft == null) {
            return;
        }
        try {
            draft.close();
        } catch (IOException error) {
            System.err.println("Não foi possível limpar o rascunho: " + error.getMessage());
        }
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
                return canEdit() && (!speakingFrame || resting != null)
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
                    CharacterImageDraft currentDraft = draft;
                    Path previous = speakingFrame ? speakingPath() : restingPath();
                    runImageTask(() -> currentDraft.importImage(file.toPath(), previous),
                            imported -> showDraftImage(speakingFrame, imported));
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

    private JButton addRemoveButton(int x, boolean speakingFrame) {
        JButton button = new JButton("Remover fundo");
        button.setFont(new Font("Roboto", Font.PLAIN, 11));
        button.setMargin(new Insets(0, 2, 0, 2));
        button.setBounds(x, 136, 110, 25);
        button.getAccessibleContext().setAccessibleName(speakingFrame
                ? "Remover fundo da imagem falando" : "Remover fundo da imagem em repouso");
        button.addActionListener(event -> removeBackground(speakingFrame));
        add(button);
        return button;
    }

    private void showDraftImage(boolean speakingFrame, CharacterImage image) {
        if (speakingFrame) {
            speaking = image;
            setPreview(speakingPreview, icon(image.image()), "");
        } else {
            resting = image;
            setPreview(restingPreview, icon(image.image()), "");
            if (speaking == null) {
                setPreview(speakingPreview, null, "Arraste a imagem");
            }
        }
    }

    private void removeBackground(boolean speakingFrame) {
        CharacterImage frame = speakingFrame ? speaking : resting;
        if (!canEdit() || frame == null) {
            return;
        }
        BufferedImage original = frame.image();
        runImageTask(() -> backgroundRemoval.remove(original), candidate -> {
            if (confirmImage(original, candidate, "Usar a imagem sem fundo?", "Original", "Sem fundo")) {
                saveProcessedImage(speakingFrame, candidate);
            }
        });
    }

    private void generateSpeakingImage() {
        if (!canEdit() || resting == null) {
            return;
        }
        BufferedImage original = resting.image();
        runImageTask(() -> generator.generate(original), candidate -> {
            if (confirmImage(original, candidate, "Usar a imagem gerada?", "Em repouso", "Falando")) {
                saveProcessedImage(true, candidate);
            }
        });
    }

    private boolean confirmImage(BufferedImage original, BufferedImage candidate, String title,
                                 String originalLabel, String candidateLabel) {
        JPanel comparison = new JPanel(new GridLayout(1, 2, 12, 0));
        comparison.add(comparisonPreview(original, originalLabel));
        comparison.add(comparisonPreview(candidate, candidateLabel));
        return JOptionPane.showConfirmDialog(this, comparison, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private JLabel comparisonPreview(BufferedImage image, String caption) {
        JLabel preview = new JLabel(caption, icon(image), SwingConstants.CENTER);
        preview.setHorizontalTextPosition(SwingConstants.CENTER);
        preview.setVerticalTextPosition(SwingConstants.TOP);
        return preview;
    }

    private void saveProcessedImage(boolean speakingFrame, BufferedImage candidate) {
        CharacterImageDraft currentDraft = draft;
        Path previous = speakingFrame ? speakingPath() : restingPath();
        runImageTask(() -> currentDraft.savePng(candidate, previous), saved -> showDraftImage(speakingFrame, saved));
    }

    private <T> void runImageTask(Callable<T> task, Consumer<T> onSuccess) {
        busy = true;
        updateControls();
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
                    updateControls();
                    onBusy.accept(false);
                }
                if (isDisplayable()) {
                    onSuccess.accept(result);
                }
                updateControls();
            }
        }.execute();
    }

    private boolean canEdit() {
        return editable && !locked && !busy;
    }

    private void updateControls() {
        generate.setEnabled(canEdit() && resting != null);
        generate.setText(busy ? "Processando imagem…" : "Gerar imagem falando");
        removeResting.setEnabled(canEdit() && resting != null);
        removeSpeaking.setEnabled(canEdit() && speaking != null);
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
