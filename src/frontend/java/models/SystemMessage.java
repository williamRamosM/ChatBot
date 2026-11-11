package models;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SystemMessage {

    private static final String API_URL = "http://127.0.0.1:8000/generate-voice-chat";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final Gson gson = new Gson();

    public ChatResponse enviarMensagem(String mensagem) throws IOException, InterruptedException {
        return enviarMensagem(mensagem, null);
    }

    public ChatResponse enviarMensagem(String mensagem, CharacterProfile profile) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", mensagem);
        if (profile != null) {
            JsonObject character = new JsonObject();
            character.addProperty("nome", profile.name());
            character.addProperty("gênero", profile.gender());
            character.addProperty("função", profile.role());
            character.addProperty("personalidade", profile.personality());
            character.addProperty("tom de voz", profile.tone());
            character.addProperty("restrições", profile.restrictions());
            payload.add("character", character);
            if (profile.voiceId() != null) {
                payload.addProperty("voice_id", profile.voiceId());
            }
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erro no servidor (" + response.statusCode() + "): " + response.body());
        }

        return gson.fromJson(response.body(), ChatResponse.class);
    }
}
