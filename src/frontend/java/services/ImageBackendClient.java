package services;

import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

final class ImageBackendClient {
    private static final String BASE_URL = "http://127.0.0.1:8000";

    BufferedImage process(String route, BufferedImage image, Duration timeout) throws IOException, InterruptedException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        ImageIO.write(image, "png", payload);

        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + route))
                .timeout(timeout)
                .header("Content-Type", "image/png")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload.toByteArray()))
                .build();
        HttpResponse<byte[]> response;
        try (HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (ConnectException error) {
            throw new IOException("Inicie o backend Python para processar a imagem.", error);
        }
        if (response.statusCode() != 200) {
            throw new IOException(errorMessage(response));
        }
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(response.body()));
        if (result == null) {
            throw new IOException("O servidor não retornou uma imagem válida.");
        }
        return result;
    }

    private String errorMessage(HttpResponse<byte[]> response) {
        try {
            return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8))
                    .getAsJsonObject().get("detail").getAsString();
        } catch (RuntimeException error) {
            return "Falha ao processar a imagem (HTTP " + response.statusCode() + ").";
        }
    }
}
