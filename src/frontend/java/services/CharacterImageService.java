package services;

import models.CharacterImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public class CharacterImageService {
    private static final Path PROFILES_DIRECTORY = Path.of("data", "personagens").toAbsolutePath();
    private static final Path IMAGES_DIRECTORY = PROFILES_DIRECTORY.resolve("imagens");

    CharacterImage importImage(Path source, Path directory) throws IOException {
        String extension = extensionOf(source);
        if (!List.of("png", "jpg", "jpeg", "webp", "gif", "bmp").contains(extension)) {
            throw new IllegalArgumentException("Escolha uma imagem PNG, JPEG, WebP, GIF ou BMP.");
        }

        Path copy = copyImage(source, directory, extension);
        try {
            return new CharacterImage(copy, readImage(copy));
        } catch (IOException | RuntimeException error) {
            discardAfterFailure(copy, error);
            throw error;
        }
    }

    Path persistDraft(Path source) throws IOException {
        return source == null ? null : copyImage(source, IMAGES_DIRECTORY, extensionOf(source));
    }

    private String extensionOf(Path source) {
        String name = source.getFileName().toString();
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private Path copyImage(Path source, Path directory, String extension) throws IOException {
        Files.createDirectories(directory);
        Path copy = Files.createTempFile(directory, "personagem-", "." + extension);
        try {
            return Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException error) {
            discardAfterFailure(copy, error);
            throw error;
        }
    }

    public BufferedImage loadImage(String relativePath) throws IOException {
        Path reference = Path.of(relativePath);
        Path path = PROFILES_DIRECTORY.resolve(reference).normalize();
        if (reference.isAbsolute() || !path.startsWith(IMAGES_DIRECTORY)) {
            throw new IOException("O caminho da imagem precisa ser relativo à pasta do perfil.");
        }
        return readImage(path);
    }

    CharacterImage savePng(BufferedImage image, Path directory) throws IOException {
        Path path = Files.createTempFile(directory, "personagem-", ".png");
        try {
            if (!ImageIO.write(image, "png", path.toFile())) {
                throw new IOException("Não foi possível gravar a imagem em PNG.");
            }
            return new CharacterImage(path, image);
        } catch (IOException | RuntimeException error) {
            discardAfterFailure(path, error);
            throw error;
        }
    }

    static void discardAfterFailure(Path path, Throwable originalError) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException | RuntimeException cleanupError) {
            originalError.addSuppressed(cleanupError);
        }
    }

    private BufferedImage readImage(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("O arquivo não é uma imagem compatível.");
        }
        return image;
    }
}
