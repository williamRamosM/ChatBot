package gui;

import models.AudioVoice;
import models.ChatResponse;
import models.SystemMessage;


import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class ScreenGuiText extends JFrame {

    SystemMessage message = new SystemMessage();
    AudioVoice audio = new AudioVoice();
    ChatResponse chatResponse = new ChatResponse();

    private int mouseX;
    private int mouseY;

    public ScreenGuiText(){

        JTextArea chat = new JTextArea();

        setLayout(null);
        setSize(500,56);
        setUndecorated(true);
        setVisible(true);

        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        chat.setFont(new Font("Roboto", Font.PLAIN, 12));

        chat.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        chat.setBounds(10, 10, 480, 36);

        chat.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    compilarMensagem("saindo [mande uma mensagem bem curta!]");
                    exit();
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String text = chat.getText();
                    if(text == null || text.trim().isEmpty()) {
                        System.out.println("System > Mensagem vazia");
                        return;
                    }
                    else{
                        chat.setText("");
                        System.out.println("System > " + text);
                    }
                    try {
                        compilarMensagem(text);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getXOnScreen();
                int y = e.getYOnScreen();

                setLocation(x - mouseX, y - mouseY);
            }
        });
        getContentPane().add(chat);
    }
    private void compilarMensagem(String mensagem) {

        SwingWorker <Void, Void> worker = new  SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    chatResponse = message.enviarMensagem(mensagem);
                    System.out.println("System > " + chatResponse.getTextResponse());
                    audio.tocarAudio(chatResponse.getAudioUrl());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return null;
            }
            @Override
            protected void done() {
                System.out.println("System > Mensagem processada com sucesso!");
            }
        };
        worker.execute();
    }

    private void exit() {
        SwingWorker <Void, Void> worker = new  SwingWorker<>() {
            @Override
            protected Void doInBackground() throws InterruptedException {
                Thread.sleep(12000);
                return null;
            }
            @Override
            protected void done() {
                System.out.println("System > Saindo...");
                System.exit(0);
            }
        };
        worker.execute();
    }
}