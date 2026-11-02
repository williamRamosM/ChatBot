package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ScreenGuiText extends JFrame {

    private int mouseX;
    private int mouseY;
    public ScreenGuiText(){

        JTextArea chat = new JTextArea();

        setLayout(null);
        setSize(500,56);
        setUndecorated(true);
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        chat.setFont(new Font("Roboto", Font.PLAIN, 12));

        chat.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        chat.setBounds(10, 10, 480, 36);

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
}
