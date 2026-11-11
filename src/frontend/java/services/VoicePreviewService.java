package services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import models.AudioVoice;
import models.ChatResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class VoicePreviewService {
    private static final URI ENDPOINT = URI.create("http://127.0.0.1:8000/preview-voice");
    private final Gson gson = new Gson();
    private AudioVoice player;

    public void play(String voiceId) throws Exception {
        JsonObject payload = new JsonObject();
        if (voiceId != null) {
            payload.addProperty("voice_id", voiceId);
        }
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        if (response.statusCode() != 200) {
            throw new IOException("O servidor recusou a prévia de voz (HTTP " + response.statusCode() + ").");
        }
        ChatResponse preview = gson.fromJson(response.body(), ChatResponse.class);
        if (preview == null || preview.getAudioUrl() == null || preview.getAudioUrl().isBlank()) {
            throw new IOException("O servidor não retornou o endereço da prévia.");
        }
        if (player == null) {
            player = new AudioVoice();
        }
        player.tocarAudio(preview.getAudioUrl());
    }
}
