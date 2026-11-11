import gui.ScreenGuiCharacter;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScreenGuiCharacter::new);
    }
}
