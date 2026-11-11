package services;

import models.CharacterImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CharacterImageDraft implements AutoCloseable {
    private final CharacterImageService images = new CharacterImageService();
    private Path directory;
    private boolean closed;

    public synchronized CharacterImage importImage(Path source, Path previous) throws IOException {
        return replace(images.importImage(source, directory()), previous);
    }

    public synchronized CharacterImage savePng(BufferedImage image, Path previous) throws IOException {
        return replace(images.savePng(image, directory()), previous);
    }

    private Path directory() throws IOException {
        if (closed) {
            throw new IOException("O rascunho já foi descartado.");
        }
        if (directory == null) {
            directory = Files.createTempDirectory("chatbot-personagem-");
            directory.toFile().deleteOnExit();
        }
        return directory;
    }

    private CharacterImage replace(CharacterImage next, Path previous) throws IOException {
        next.path().toFile().deleteOnExit();
        try {
            if (previous != null) {
                if (!directory.equals(previous.getParent())) {
                    throw new IOException("A imagem anterior não pertence a este rascunho.");
                }
                Files.deleteIfExists(previous);
            }
            return next;
        } catch (IOException | RuntimeException error) {
            CharacterImageService.discardAfterFailure(next.path(), error);
            throw error;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
        if (directory == null || Files.notExists(directory)) {
            return;
        }
        try (var files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
        }
        Files.deleteIfExists(directory);
    }
}
