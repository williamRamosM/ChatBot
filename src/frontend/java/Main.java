import models.AudioVoice;
import models.ChatResponse;
import models.SystemMessage;

public class Main {
    public static void main(String[] args) {
        SystemMessage chat = new SystemMessage();
        AudioVoice audio = new AudioVoice();

        try {
            ChatResponse resposta = chat.enviarMensagem("Oi Luna, tudo bem?");
            System.out.println(resposta.getTextResponse());
            audio.tocarAudio(resposta.getAudioUrl());
        } catch (Exception e) {
            System.err.println("Erro: " + e);
            e.printStackTrace();
        }
    }
}