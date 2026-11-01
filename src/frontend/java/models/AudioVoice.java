package models;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class AudioVoice {

    private static final String BASE_URL = "http://127.0.0.1:8000";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private volatile Clip clip;

    // Toca e só retorna quando o áudio terminar
    public void tocarAudio(String audioUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + audioUrl))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Falha ao baixar o áudio: " + response.statusCode());
        }

        try (InputStream in = new BufferedInputStream(response.body());
             AudioInputStream ais = AudioSystem.getAudioInputStream(in)) {

            clip = AudioSystem.getClip();
            CountDownLatch terminou = new CountDownLatch(1);
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    terminou.countDown();
                }
            });

            clip.open(ais);
            clip.start();
            terminou.await();
            clip.close();
        }
    }

    // Toca em segundo plano (use com interface gráfica)
    public void tocarAudioAsync(String audioUrl) {
        new Thread(() -> {
            try {
                tocarAudio(audioUrl);
            } catch (Exception e) {
                System.err.println("Erro ao tocar áudio: " + e);
            }
        }, "audio-luna").start();
    }

    public void parar() {
        Clip c = clip;
        if (c != null) {
            c.stop();
        }
    }
}