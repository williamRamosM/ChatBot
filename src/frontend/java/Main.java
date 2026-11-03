import gui.ScreenGuiText;
import models.AudioVoice;
import models.ChatResponse;
import models.SystemMessage;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        SystemMessage chat = new SystemMessage();
        AudioVoice audio = new AudioVoice();
        ScreenGuiText gui = new ScreenGuiText();

//        Scanner input = new Scanner(System.in);
//        String text;
//        while(true){
//            text = input.nextLine();
//            try {
//                ChatResponse resposta = chat.enviarMensagem(text);
//                System.out.println(resposta.getTextResponse());
//                audio.tocarAudio(resposta.getAudioUrl());
//            } catch (Exception e) {
//                System.err.println("Erro: " + e);
//                e.printStackTrace();
//            }
//        }

    }
}