package services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import models.CharacterProfile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CharacterProfileService {
    private static final Path DIRECTORY = Path.of("data", "personagens").toAbsolutePath();
    private static final Path PROFILES = DIRECTORY.resolve("personagens.json");
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    public String relativeImagePath(Path image) {
        if (image == null) {
            return null;
        }
        return DIRECTORY.relativize(image.toAbsolutePath()).toString().replace('\\', '/');
    }

    public synchronized void append(CharacterProfile profile) throws IOException {
        JsonArray profiles = readProfiles();
        profiles.add(GSON.toJsonTree(profile));
        writeProfiles(profiles);
    }

    public synchronized void delete(CharacterProfile selected) throws IOException {
        JsonArray profiles = readProfiles();
        for (int index = 0; index < profiles.size(); index++) {
            CharacterProfile saved = GSON.fromJson(profiles.get(index), CharacterProfile.class);
            if (selected.equals(saved)) {
                profiles.remove(index);
                writeProfiles(profiles);
                return;
            }
        }
        throw new IOException("O perfil de \"" + selected.name() + "\" não foi encontrado no JSON.");
    }

    private void writeProfiles(JsonArray profiles) throws IOException {
        Files.createDirectories(DIRECTORY);
        Path temporary = Files.createTempFile(DIRECTORY, "personagens-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(profiles) + System.lineSeparator());
            Files.move(temporary, PROFILES, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public List<CharacterProfile> listProfiles() throws IOException {
        List<CharacterProfile> profiles = new ArrayList<>();
        try {
            for (var entry : readProfiles()) {
                CharacterProfile profile = GSON.fromJson(entry, CharacterProfile.class);
                if (profile != null) {
                    profiles.add(profile);
                }
            }
        } catch (JsonParseException error) {
            throw new IOException("O arquivo personagens.json contém um perfil inválido.", error);
        }
        return profiles;
    }

    private JsonArray readProfiles() throws IOException {
        if (Files.notExists(PROFILES)) {
            copyInitialResource("imagens/nasa.png");
            copyInitialResource("imagens/nasa-falando.png");
            copyInitialResource("personagens.json");
        }
        try (var reader = Files.newBufferedReader(PROFILES)) {
            JsonArray profiles = GSON.fromJson(reader, JsonArray.class);
            if (profiles == null) {
                throw new IOException("O arquivo personagens.json precisa conter uma lista.");
            }
            return profiles;
        } catch (JsonParseException error) {
            throw new IOException("O arquivo personagens.json precisa conter uma lista válida.", error);
        }
    }

    private void copyInitialResource(String relativePath) throws IOException {
        Path destination = DIRECTORY.resolve(relativePath);
        if (Files.exists(destination)) {
            return;
        }

        Path parent = destination.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "personagem-inicial-", ".tmp");
        try (var resource = CharacterProfileService.class.getResourceAsStream("/personagens/" + relativePath)) {
            if (resource == null) {
                throw new IOException("Recurso inicial não encontrado: " + relativePath);
            }
            Files.copy(resource, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, destination);
        } catch (FileAlreadyExistsException ignored) {
            // Outra instância já copiou o recurso.
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
