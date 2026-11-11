package models;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

public record CharacterImage(Path path, BufferedImage image) {
}
