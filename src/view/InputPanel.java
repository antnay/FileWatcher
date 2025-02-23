package view;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.*;
import java.beans.PropertyChangeSupport;

public class InputPanel extends JPanel {
    private PropertyChangeSupport myPCS;

    public InputPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        initInputFields();
    }

    private void initInputFields() {
        BorderLayout layout = new BorderLayout();
        layout.setVgap(10);
        layout.setHgap(10);
        setLayout(layout);

        // label telling the user what to do
        JLabel instructionLabel = new JLabel("Select a file extension, a directory, and click Start to begin File Watcher.");
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(instructionLabel, BorderLayout.NORTH);

        // add the input fields for file extension and file directory
        add(createExtensionPanel(), BorderLayout.WEST);
        add(createDirectoryPanel(), BorderLayout.EAST);

        // start and stop buttons
    }

    private JPanel createExtensionPanel() {
        // label for extension combo box
        JLabel extensionLabel = new JLabel("Monitor by extension");

        // combo box for extension
        String[] commonExtensions = {
                ".txt",
                ".exe",
                ".jar"
        };

        JComboBox<String> extensionList = new JComboBox<>(commonExtensions);
        extensionList.setEditable(true);

        JPanel extensionPanel = new JPanel(new BorderLayout());
        extensionPanel.add(extensionLabel, BorderLayout.NORTH);
        extensionPanel.add(extensionList, BorderLayout.CENTER);

        return extensionPanel;
    }

    private JPanel createDirectoryPanel() {
        // label for directory field
        JLabel directoryLabel = new JLabel("Directory to monitor");

        // text field for directory
        JTextField directoryField = new JTextField();
        directoryField.setColumns(40);

        JPanel directoryPanel = new JPanel(new BorderLayout());
        directoryPanel.add(directoryLabel, BorderLayout.NORTH);
        directoryPanel.add(directoryField, BorderLayout.CENTER);


        return directoryPanel;
    }
}
