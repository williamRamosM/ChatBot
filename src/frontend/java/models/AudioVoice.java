package models;

import javax.sound.sampled.AudioFormat;
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

    private volatile Playback playback;

    private record Playback(Clip clip, SpeechLevels levels) {
    }

    // Toca e só retorna quando o áudio terminar
    public synchronized void tocarAudio(String audioUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + audioUrl))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream in = new BufferedInputStream(response.body())) {
            if (response.statusCode() != 200) {
                throw new IOException("Falha ao baixar o áudio: " + response.statusCode());
            }
            try (AudioInputStream source = AudioSystem.getAudioInputStream(in)) {
                tocarPcm(source);
            }
        }
    }

    private void tocarPcm(AudioInputStream source) throws Exception {
        AudioFormat original = source.getFormat();
        AudioFormat pcm = new AudioFormat(original.getSampleRate(), 16, original.getChannels(), true, false);
        try (AudioInputStream samples = AudioSystem.getAudioInputStream(pcm, source);
             Clip clip = AudioSystem.getClip()) {
            byte[] audio = samples.readAllBytes();
            if (audio.length == 0) {
                throw new IOException("O áudio recebido está vazio.");
            }
            SpeechLevels levels = SpeechLevels.fromPcm16(audio, pcm);
            CountDownLatch terminou = new CountDownLatch(1);
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    terminou.countDown();
                }
            });

            clip.open(pcm, audio, 0, audio.length);
            playback = new Playback(clip, levels);
            try {
                clip.start();
                terminou.await();
            } finally {
                playback = null;
            }
        }
    }

    public double speechIntensity() {
        Playback current = playback;
        if (current == null || !current.clip().isRunning()) {
            return 0;
        }
        return current.levels().atFrame(current.clip().getLongFramePosition());
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
        Playback current = playback;
        if (current != null) {
            current.clip().stop();
        }
    }
}
