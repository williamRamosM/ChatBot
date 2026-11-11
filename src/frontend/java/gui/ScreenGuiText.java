package gui;

import models.AudioVoice;
import models.CharacterProfile;
import models.ChatResponse;
import models.SystemMessage;
import services.CharacterImageService;


import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ScreenGuiText extends JFrame {
    private static final int AVATAR_SPACE = 180;
    private static final int CHAT_WIDTH = 300;
    private static final int CHAT_HEIGHT = 30;

    SystemMessage message = new SystemMessage();
    AudioVoice audio = new AudioVoice();

    private int mouseX;
    private int mouseY;
    private final CharacterProfile activeProfile;

    private record AvatarImages(BufferedImage resting, BufferedImage speaking) {
    }

    public ScreenGuiText() {
        this(null);
    }

    public ScreenGuiText(CharacterProfile profile) {
        activeProfile = profile;
        if (activeProfile != null) {
            setTitle(activeProfile.name());
        }

        JTextArea chat = new JTextArea();

        setLayout(null);
        setSize(500,56);
        setUndecorated(true);

        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        chat.setOpaque(true);
        chat.setFont(new Font("Roboto", Font.PLAIN, 12));

        chat.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        chat.setBounds((getWidth() - CHAT_WIDTH) / 2, 10, CHAT_WIDTH, CHAT_HEIGHT);

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
        setVisible(true);
        if (activeProfile != null && activeProfile.imagePath() != null && !activeProfile.imagePath().isBlank()) {
            loadAvatar(activeProfile, chat);
        }
    }

    private void loadAvatar(CharacterProfile profile, JTextArea chat) {
        new SwingWorker<AvatarImages, Void>() {
            @Override
            protected AvatarImages doInBackground() throws IOException {
                CharacterImageService images = new CharacterImageService();
                BufferedImage resting = images.loadImage(profile.imagePath());
                BufferedImage speaking = null;
                if (profile.speakingImagePath() != null && !profile.speakingImagePath().isBlank()) {
                    try {
                        speaking = images.loadImage(profile.speakingImagePath());
                    } catch (IOException | IllegalArgumentException error) {
                        System.err.println("Não foi possível carregar a imagem falando: " + error.getMessage());
                    }
                }
                return new AvatarImages(resting, speaking);
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    AvatarImages images = get();
                    CharacterAvatar avatar = new CharacterAvatar(images.resting(), images.speaking(), audio::speechIntensity);
                    avatar.setBounds(10, 10, 480, AVATAR_SPACE - 10);
                    chat.setLocation(chat.getX(), chat.getY() + AVATAR_SPACE);
                    getContentPane().add(avatar);
                    GraphicsDevice display = getGraphicsConfiguration().getDevice();
                    if (display.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
                        setBackground(new Color(0, 0, 0, 0));
                    }
                    setSize(getWidth(), getHeight() + AVATAR_SPACE);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    System.err.println("Não foi possível carregar o avatar: " + error.getCause().getMessage());
                }
            }
        }.execute();
    }
    private void compilarMensagem(String mensagem) {

        SwingWorker <Void, Void> worker = new  SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    ChatResponse chatResponse = message.enviarMensagem(mensagem);
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
