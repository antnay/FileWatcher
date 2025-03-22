package view;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.awt.*;

/**
 * A window that displays information about the program. Provides information about how to use it,
 * the current version, and the contributors.
 */
class HelpFrame extends JFrame {
    /**
     * Constructs a window that tells the user information about the program and displays it.
     */
    public HelpFrame() {
        setTitle("About");
        setLocationRelativeTo(null);

        JTextArea aboutText = new JTextArea("""
                This program will monitor events on files with the specified extension types and will display them on the window as they occur. \n
                To monitor files, add an extension to the 'Monitor by extension' field and click the start button. \n
                FileWatcher version 1.0 by Anthony Naydyuk, Yosan Tesfay, and Mitchell Nowasky
                """);
        aboutText.setLineWrap(true);
        aboutText.setWrapStyleWord(true);
        aboutText.setEditable(false);
        aboutText.setFocusable(false);
        aboutText.setFont(new Font("Arial", Font.PLAIN, 16));
        aboutText.setPreferredSize(new Dimension(500, 200));
        add(aboutText);
        pack();

        setVisible(true);
    }
}
