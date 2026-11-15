import gui.ScreenGuiCharacter;
import services.BackendManeger;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        BackendManeger.startBackend();
        SwingUtilities.invokeLater(ScreenGuiCharacter::new);
    }
}
