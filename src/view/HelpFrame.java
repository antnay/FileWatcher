package view;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.awt.*;

public class HelpFrame extends JFrame {
    public HelpFrame() {
        setTitle("About");
        setLocationRelativeTo(null);

        JTextArea aboutText = new JTextArea("""
                This program will monitor events on files with the specified extension types and will display them on the window as they occur. \
                To monitor files, add an extension to the 'Monitor by extension' field and click the start button.
                
                FileWatcher version 1.0 by Anthony Naydyuk, Yosan Tesfay, and Mitchell Nowasky
                """);
        aboutText.setLineWrap(true);
        aboutText.setWrapStyleWord(true);
        aboutText.setEditable(false);
        aboutText.setFont(new Font("Arial", Font.PLAIN, 16));
        aboutText.setPreferredSize(new Dimension(500, 200));
        add(aboutText);
        pack();

        setVisible(true);
    }
}
