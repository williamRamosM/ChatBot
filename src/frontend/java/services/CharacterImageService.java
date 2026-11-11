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

    public CharacterImage importImage(Path source) throws IOException {
        String name = source.getFileName().toString();
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!List.of("png", "jpg", "jpeg", "webp", "gif", "bmp").contains(extension)) {
            throw new IllegalArgumentException("Escolha uma imagem PNG, JPEG, WebP, GIF ou BMP.");
        }

        BufferedImage image = readImage(source);
        Files.createDirectories(IMAGES_DIRECTORY);
        Path savedPath = Files.createTempFile(IMAGES_DIRECTORY, "personagem-", "." + extension);
        try {
            Files.copy(source, savedPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            try {
                Files.deleteIfExists(savedPath);
            } catch (IOException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
        return new CharacterImage(savedPath, image);
    }

    public BufferedImage loadImage(String relativePath) throws IOException {
        Path reference = Path.of(relativePath);
        Path path = PROFILES_DIRECTORY.resolve(reference).normalize();
        if (reference.isAbsolute() || !path.startsWith(IMAGES_DIRECTORY)) {
            throw new IOException("O caminho da imagem precisa ser relativo à pasta do perfil.");
        }
        return readImage(path);
    }

    private BufferedImage readImage(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("O arquivo não é uma imagem compatível.");
        }
        return image;
    }
}
