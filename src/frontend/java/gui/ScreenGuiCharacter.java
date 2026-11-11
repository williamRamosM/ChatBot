package gui;

import models.CharacterImage;
import models.CharacterProfile;
import services.CharacterImageService;
import services.CharacterProfileService;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ScreenGuiCharacter extends JFrame {

    private static final String DEFAULT_VOICE = "Voz padrão";

    private int mouseX;
    private int mouseY;
    private boolean importingImage;
    private boolean updatingProfiles;
    private Path selectedImage;
    private final CharacterImageService imageService = new CharacterImageService();
    private final CharacterProfileService profileService = new CharacterProfileService();
    private final JComboBox<ProfileOption> characters = new JComboBox<>();
    private final JButton delete = roundedButton("Deletar", "personagens", new Color(185, 28, 28));
    private final JButton start = roundedButton("Iniciar", "personagem", new Color(29, 78, 216));
    private final JTextField name;
    private final JTextField role;
    private final JTextArea personality;
    private final JComboBox<String> tone;
    private final JComboBox<String> voice;
    private final JTextArea restrictions;
    private final JLabel image = new JLabel("Arraste a imagem", SwingConstants.CENTER);

    private record ProfileOption(CharacterProfile profile) {
        @Override
        public String toString() {
            if (profile == null) {
                return "Novo personagem";
            }
            String name = profile.name();
            return name == null || name.isBlank() ? "Personagem sem nome" : name;
        }
    }

    public ScreenGuiCharacter() {
        setTitle("Criar personagem");
        setLayout(null);
        setSize(500, 635);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        addLabel("Personagens:", 20);
        characters.addItem(new ProfileOption(null));
        characters.setFont(new Font("Roboto", Font.PLAIN, 12));
        characters.setBounds(140, 20, 340, 30);
        add(characters);

        name = addTextField("Nome:", 65);
        role = addTextField("Função:", 110);
        personality = addTextArea("Personalidade:", 155);
        tone = addDropdown("Tom de voz:", 240,
                "Calmo", "Animado", "Sério", "Acolhedor", "Firme", "Provocador", "Ríspido");
        tone.setSelectedIndex(-1);
        voice = addDropdown("Voz do personagem:", 285, DEFAULT_VOICE);
        restrictions = addTextArea("Restrições:", 330);

        addLabel("Img personagem:", 415);
        image.setFont(new Font("Roboto", Font.PLAIN, 12));
        image.setForeground(Color.GRAY);
        image.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        image.setBounds(140, 415, 110, 110);
        add(image);

        TransferHandler imageDrop = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return characters.getSelectedIndex() == 0 && !importingImage && !updatingProfiles
                        && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    List<?> files = (List<?>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.size() != 1 || !(files.getFirst() instanceof File file) || !file.isFile()) {
                        throw new IllegalArgumentException("Arraste apenas um arquivo de imagem por vez.");
                    }
                    importImage(file.toPath(), image);
                    return true;
                } catch (Exception error) {
                    JOptionPane.showMessageDialog(ScreenGuiCharacter.this, error.getMessage(),
                            "Erro ao receber imagem", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        };
        image.setTransferHandler(imageDrop);

        delete.setBounds(20, 575, 92, 36);
        delete.setEnabled(false);
        delete.addActionListener(event -> deleteSelectedProfile());
        add(delete);

        start.setBounds(388, 575, 92, 36);
        start.addActionListener(event -> {
            ProfileOption option = (ProfileOption) characters.getSelectedItem();
            if (option.profile() != null) {
                openChat(option.profile());
                return;
            }
            if (importingImage) {
                JOptionPane.showMessageDialog(this, "Aguarde a importação da imagem.");
                return;
            }
            if (!requireText(name, "Nome")
                    || !requireText(role, "Função")
                    || !requireText(personality, "Personalidade")) {
                return;
            }
            String selectedTone = (String) tone.getSelectedItem();
            if (selectedTone == null) {
                JOptionPane.showMessageDialog(this, "Selecione um tom de voz.",
                        "Campo obrigatório", JOptionPane.WARNING_MESSAGE);
                tone.requestFocusInWindow();
                return;
            }
            if (!requireText(restrictions, "Restrições")) {
                return;
            }
            CharacterProfile profile = new CharacterProfile(name.getText(), role.getText(),
                    personality.getText(), selectedTone, null, restrictions.getText(),
                    profileService.relativeImagePath(selectedImage));
            updateProfiles(() -> profileService.append(profile), () -> openChat(profile),
                    "Não foi possível salvar o personagem.");
        });
        add(start);
        getRootPane().setDefaultButton(start);

        MouseAdapter dragWindow = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                mouseX = event.getX();
                mouseY = event.getY();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                setLocation(event.getXOnScreen() - mouseX, event.getYOnScreen() - mouseY);
            }
        };
        addMouseListener(dragWindow);
        addMouseMotionListener(dragWindow);

        characters.addActionListener(event -> showProfile((ProfileOption) characters.getSelectedItem()));
        setVisible(true);
        loadProfiles();
    }

    private static JButton roundedButton(String firstLine, String secondLine, Color color) {
        JButton button = new JButton("<html><center>" + firstLine + "<br>" + secondLine + "</center></html>") {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D background = (Graphics2D) graphics.create();
                background.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                background.setColor(color);
                if (!isEnabled()) {
                    background.setComposite(AlphaComposite.SrcOver.derive(0.45f));
                }
                background.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                background.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setFont(new Font("Roboto", Font.PLAIN, 11));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.getAccessibleContext().setAccessibleName(firstLine + " " + secondLine);
        return button;
    }

    private void loadProfiles() {
        try {
            for (CharacterProfile profile : profileService.listProfiles()) {
                characters.addItem(new ProfileOption(profile));
            }
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, "Não foi possível carregar os personagens. " + error.getMessage(),
                    "Erro ao carregar personagens", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showProfile(ProfileOption option) {
        CharacterProfile profile = option.profile();
        boolean creating = profile == null;
        delete.setEnabled(!creating && !updatingProfiles);
        List<JTextComponent> fields = List.of(name, role, personality, restrictions);
        fields.forEach(field -> field.setEditable(creating));
        tone.setEnabled(creating);
        voice.setEnabled(creating);
        selectedImage = null;
        image.setIcon(null);

        if (creating) {
            fields.forEach(field -> field.setText(""));
            tone.setSelectedIndex(-1);
            voice.setSelectedIndex(0);
            image.setText("Arraste a imagem");
            return;
        }

        name.setText(profile.name());
        role.setText(profile.role());
        personality.setText(profile.personality());
        tone.getModel().setSelectedItem(profile.tone());
        voice.getModel().setSelectedItem(profile.voiceId() == null ? DEFAULT_VOICE : profile.voiceId());
        restrictions.setText(profile.restrictions());
        if (profile.imagePath() == null || profile.imagePath().isBlank()) {
            image.setText("Sem imagem");
            return;
        }

        image.setText("Carregando imagem");
        loadPreview(option);
    }

    private void loadPreview(ProfileOption option) {
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws IOException {
                return createPreview(imageService.loadImage(option.profile().imagePath()));
            }

            @Override
            protected void done() {
                if (characters.getSelectedItem() != option || !isDisplayable()) {
                    return;
                }
                try {
                    image.setIcon(get());
                    image.setText(null);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    image.setText("Imagem indisponível");
                } catch (ExecutionException error) {
                    image.setText("Imagem indisponível");
                }
            }
        }.execute();
    }

    private void openChat(CharacterProfile profile) {
        ScreenGuiText chat = new ScreenGuiText(profile);
        chat.setLocation(getLocation());
        dispose();
    }

    private boolean requireText(JTextComponent field, String label) {
        if (!field.getText().isBlank()) {
            return true;
        }
        JOptionPane.showMessageDialog(this, "Preencha o campo \"" + label + "\".",
                "Campo obrigatório", JOptionPane.WARNING_MESSAGE);
        field.requestFocusInWindow();
        return false;
    }

    private void deleteSelectedProfile() {
        int index = characters.getSelectedIndex();
        if (index <= 0) {
            return;
        }
        CharacterProfile profile = characters.getItemAt(index).profile();
        updateProfiles(() -> profileService.delete(profile), () -> {
            characters.setSelectedIndex(0);
            characters.removeItemAt(index);
        }, "Não foi possível excluir o personagem.");
    }

    @FunctionalInterface
    private interface ProfileUpdate {
        void run() throws IOException;
    }

    private void updateProfiles(ProfileUpdate update, Runnable onSuccess, String errorMessage) {
        updatingProfiles = true;
        characters.setEnabled(false);
        start.setEnabled(false);
        delete.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                update.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException error) {
                    JOptionPane.showMessageDialog(ScreenGuiCharacter.this,
                            errorMessage + " " + error.getCause().getMessage(),
                            "Erro ao atualizar personagens", JOptionPane.ERROR_MESSAGE);
                    return;
                } finally {
                    updatingProfiles = false;
                    characters.setEnabled(true);
                    start.setEnabled(true);
                    delete.setEnabled(characters.getSelectedIndex() > 0);
                }
                onSuccess.run();
            }
        }.execute();
    }

    private void importImage(Path source, JLabel preview) {
        importingImage = true;
        characters.setEnabled(false);
        SwingWorker<CharacterImage, Void> worker = new SwingWorker<>() {
            @Override
            protected CharacterImage doInBackground() throws Exception {
                return imageService.importImage(source);
            }

            @Override
            protected void done() {
                try {
                    CharacterImage imported = get();
                    preview.setIcon(createPreview(imported.image()));
                    preview.setText(null);
                    selectedImage = imported.path();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    JOptionPane.showMessageDialog(ScreenGuiCharacter.this,
                            "Não foi possível importar a imagem. " + error.getCause().getMessage(),
                            "Erro ao importar imagem", JOptionPane.ERROR_MESSAGE);
                } finally {
                    importingImage = false;
                    characters.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private ImageIcon createPreview(BufferedImage image) {
        double scale = Math.min(106.0 / image.getWidth(), 106.0 / image.getHeight());
        Image thumbnail = image.getScaledInstance(
                Math.max(1, (int) (image.getWidth() * scale)),
                Math.max(1, (int) (image.getHeight() * scale)), Image.SCALE_SMOOTH);
        return new ImageIcon(thumbnail);
    }

    private void addLabel(String text, int y) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Roboto", Font.PLAIN, 12));
        label.setBounds(20, y, 120, 30);
        add(label);
    }

    private JTextField addTextField(String label, int y) {
        addLabel(label, y);
        JTextField field = new JTextField();
        field.setFont(new Font("Roboto", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        field.setBounds(140, y, 340, 30);
        add(field);
        return field;
    }

    private JComboBox<String> addDropdown(String label, int y, String... options) {
        addLabel(label, y);
        JComboBox<String> dropdown = new JComboBox<>(options);
        dropdown.setFont(new Font("Roboto", Font.PLAIN, 12));
        dropdown.setBounds(140, y, 340, 30);
        add(dropdown);
        return dropdown;
    }

    private JTextArea addTextArea(String label, int y) {
        addLabel(label, y);
        JTextArea field = new JTextArea();
        field.setFont(new Font("Roboto", Font.PLAIN, 12));
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(field);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scroll.setBounds(140, y, 340, 65);
        add(scroll);
        return field;
    }
}
