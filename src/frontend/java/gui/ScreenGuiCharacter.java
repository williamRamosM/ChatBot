package gui;

import models.CharacterProfile;
import services.CharacterProfileService;
import services.VoicePreviewService;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class ScreenGuiCharacter extends JFrame {

    private static final List<VoiceOption> VOICES = List.of(
            new VoiceOption("Voz padrão (Francisca)", null),
            new VoiceOption("Antônio", "pt-BR-AntonioNeural"),
            new VoiceOption("Thalita", "pt-BR-ThalitaMultilingualNeural")
    );
    private static final String OTHER_GENDER = "Outro";
    private static final List<String> STANDARD_GENDERS = List.of("Feminino", "Masculino", "Neutro");

    private int mouseX;
    private int mouseY;
    private boolean importingImage;
    private boolean updatingProfiles;
    private final CharacterProfileService profileService = new CharacterProfileService();
    private final JComboBox<ProfileOption> characters = new JComboBox<>();
    private final JButton delete = roundedButton("Deletar", "personagens", new Color(185, 28, 28));
    private final JButton start = roundedButton("Iniciar", "personagem", new Color(29, 78, 216));
    private final JButton listen = new JButton("Ouvir voz");
    private final VoicePreviewService voicePreview = new VoicePreviewService();
    private SwingWorker<Void, Void> previewWorker;
    private final JTextField name;
    private final JComboBox<String> gender;
    private final JTextField customGender;
    private final JTextField role;
    private final JTextArea personality;
    private final JComboBox<String> tone;
    private final JComboBox<VoiceOption> voice;
    private final JTextArea restrictions;
    private final CharacterImagesPanel images = new CharacterImagesPanel(busy -> {
        importingImage = busy;
        characters.setEnabled(!busy);
    });

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

    private record VoiceOption(String label, String id) {
        @Override
        public String toString() {
            return label;
        }
    }

    public ScreenGuiCharacter() {
        setTitle("Criar personagem");
        setLayout(null);
        setSize(500, 660);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        addLabel("Personagens:", 20);
        characters.addItem(new ProfileOption(null));
        characters.setFont(new Font("Roboto", Font.PLAIN, 12));
        characters.setBounds(140, 20, 340, 30);
        add(characters);

        name = addTextField("Nome:", 65);
        gender = addDropdown("Gênero:", 105, STANDARD_GENDERS.toArray(String[]::new));
        gender.addItem(OTHER_GENDER);
        gender.setSelectedIndex(-1);
        gender.setBounds(140, 105, 145, 30);
        customGender = addTextField(295, 105, 185);
        customGender.getAccessibleContext().setAccessibleName("Gênero personalizado");
        customGender.setVisible(false);
        gender.addActionListener(event -> customGender.setVisible(OTHER_GENDER.equals(gender.getSelectedItem())));

        role = addTextField("Função:", 145);
        personality = addTextArea("Personalidade:", 185);
        tone = addDropdown("Tom de voz:", 260, new String[]{
                "Calmo", "Animado", "Sério", "Acolhedor", "Firme", "Provocador", "Ríspido"});
        tone.setSelectedIndex(-1);
        voice = addDropdown("Voz do personagem:", 300, VOICES.toArray(VoiceOption[]::new));
        voice.setBounds(140, 300, 220, 30);
        listen.setFont(new Font("Roboto", Font.PLAIN, 12));
        listen.setBounds(370, 300, 110, 30);
        listen.addActionListener(event -> listenToVoice());
        add(listen);
        restrictions = addTextArea("Restrições:", 340);

        images.setBounds(20, 415, 460, 162);
        add(images);

        delete.setBounds(20, 600, 92, 36);
        delete.setEnabled(false);
        delete.addActionListener(event -> deleteSelectedProfile());
        add(delete);

        start.setBounds(388, 600, 92, 36);
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
            if (!requireText(name, "Nome")) {
                return;
            }
            String selectedGender = readGender();
            if (selectedGender == null || !requireText(role, "Função")
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
            VoiceOption selectedVoice = (VoiceOption) voice.getSelectedItem();
            CharacterProfile profile = new CharacterProfile(name.getText(), selectedGender, role.getText(),
                    personality.getText(), selectedTone, selectedVoice.id(), restrictions.getText(), null, null);
            Path resting = images.restingPath();
            Path speaking = images.speakingPath();
            updateProfiles(() -> profileService.create(profile, resting, speaking), saved -> {
                images.showProfile(null);
                openChat(saved);
            },
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
        gender.setEnabled(creating);
        customGender.setEditable(creating);
        tone.setEnabled(creating);
        voice.setEnabled(creating);
        images.showProfile(profile);
        showGender(creating ? null : profile.gender());

        if (creating) {
            fields.forEach(field -> field.setText(""));
            tone.setSelectedIndex(-1);
            voice.setSelectedIndex(0);
            return;
        }

        name.setText(profile.name());
        role.setText(profile.role());
        personality.setText(profile.personality());
        tone.getModel().setSelectedItem(profile.tone());
        String voiceId = "pt-BR-FranciscaNeural".equals(profile.voiceId()) ? null : profile.voiceId();
        VoiceOption savedVoice = VOICES.stream()
                .filter(optionVoice -> Objects.equals(optionVoice.id(), voiceId))
                .findFirst()
                .orElseGet(() -> new VoiceOption(voiceId, voiceId));
        voice.getModel().setSelectedItem(savedVoice);
        restrictions.setText(profile.restrictions());
    }

    private void showGender(String value) {
        String saved = value == null || value.isBlank() ? null : value;
        boolean custom = saved != null && !STANDARD_GENDERS.contains(saved);
        gender.setSelectedItem(custom ? OTHER_GENDER : saved);
        customGender.setText(custom ? saved : "");
    }

    private String readGender() {
        String selected = (String) gender.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecione um gênero.", "Campo obrigatório", JOptionPane.WARNING_MESSAGE);
            gender.requestFocusInWindow();
            return null;
        }
        if (OTHER_GENDER.equals(selected)) {
            return requireText(customGender, "Gênero personalizado") ? customGender.getText().strip() : null;
        }
        return selected;
    }

    private void openChat(CharacterProfile profile) {
        ScreenGuiText chat = new ScreenGuiText(profile);
        chat.setLocation(getLocation());
        dispose();
    }

    @Override
    public void dispose() {
        if (!updatingProfiles) {
            if (previewWorker != null) {
                previewWorker.cancel(true);
            }
            images.discardDraft();
            super.dispose();
        }
    }

    private void listenToVoice() {
        VoiceOption selected = (VoiceOption) voice.getSelectedItem();
        if (selected == null || previewWorker != null) {
            return;
        }
        listen.setEnabled(false);
        listen.setText("Aguarde…");
        previewWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                voicePreview.play(selected.id());
                return null;
            }

            @Override
            protected void done() {
                if (isCancelled() || !isDisplayable()) {
                    return;
                }
                previewWorker = null;
                listen.setText("Ouvir voz");
                listen.setEnabled(true);
                try {
                    get();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    JOptionPane.showMessageDialog(ScreenGuiCharacter.this,
                            "Não foi possível ouvir a voz. " + error.getCause().getMessage(),
                            "Erro na prévia de voz", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        previewWorker.execute();
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
        updateProfiles(() -> {
            profileService.delete(profile);
            return null;
        }, ignored -> {
            characters.setSelectedIndex(0);
            characters.removeItemAt(index);
        }, "Não foi possível excluir o personagem.");
    }

    @FunctionalInterface
    private interface ProfileUpdate<T> {
        T run() throws IOException;
    }

    private <T> void updateProfiles(ProfileUpdate<T> update, Consumer<T> onSuccess, String errorMessage) {
        updatingProfiles = true;
        images.setLocked(true);
        characters.setEnabled(false);
        start.setEnabled(false);
        delete.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws IOException {
                return update.run();
            }

            @Override
            protected void done() {
                T result;
                try {
                    result = get();
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
                    images.setLocked(false);
                    characters.setEnabled(true);
                    start.setEnabled(true);
                    delete.setEnabled(characters.getSelectedIndex() > 0);
                }
                onSuccess.accept(result);
            }
        }.execute();
    }

    private void addLabel(String text, int y) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Roboto", Font.PLAIN, 12));
        label.setBounds(20, y, 120, 30);
        add(label);
    }

    private JTextField addTextField(String label, int y) {
        addLabel(label, y);
        return addTextField(140, y, 340);
    }

    private JTextField addTextField(int x, int y, int width) {
        JTextField field = new JTextField();
        field.setFont(new Font("Roboto", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        field.setBounds(x, y, width, 30);
        add(field);
        return field;
    }

    private <T> JComboBox<T> addDropdown(String label, int y, T[] options) {
        addLabel(label, y);
        JComboBox<T> dropdown = new JComboBox<>(options);
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
