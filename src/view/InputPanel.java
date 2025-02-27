package view;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.*;
import java.beans.PropertyChangeSupport;

public class InputPanel extends JPanel {
    private PropertyChangeSupport myPCS;
    private JComboBox<String> myComboBox;
    private JTextField myTextField;

    public InputPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        initInputFields();
    }

    private void initInputFields() {
        BorderLayout layout = new BorderLayout(10, 10);
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

        myComboBox = new JComboBox<>(commonExtensions);
        myComboBox.setEditable(true);

        JPanel extensionPanel = new JPanel(new BorderLayout());
        extensionPanel.add(extensionLabel, BorderLayout.NORTH);

        // nest a border layout inside extensionPanel to
        JPanel nestedPanel = new JPanel(new BorderLayout());
        extensionPanel.add(nestedPanel, BorderLayout.CENTER);
        nestedPanel.add(myComboBox, BorderLayout.NORTH);

        return extensionPanel;
    }

    private JPanel createDirectoryPanel() {
        // label for directory field
        JLabel directoryLabel = new JLabel("Directory to monitor");

        // text field for directory
        myTextField = new JTextField();
        myTextField.setColumns(40);

        JPanel directoryPanel = new JPanel(new BorderLayout());
        directoryPanel.add(directoryLabel, BorderLayout.NORTH);
        directoryPanel.add(myTextField, BorderLayout.CENTER);
        directoryPanel.add(initButtons(), BorderLayout.SOUTH);

        return directoryPanel;
    }

    private JPanel initButtons() {
        JPanel buttonGrid = new JPanel(new GridLayout());

        JButton startButton = new JButton("Start");
        startButton.addActionListener(theEvent -> {
            String[] inputFields = {(String) myComboBox.getSelectedItem(), myTextField.getText()};
            myPCS.firePropertyChange(ViewProperties.START_BUTTON, null, inputFields);
        });
        buttonGrid.add(startButton);

        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(theEvent -> {
            String[] inputFields = {(String) myComboBox.getSelectedItem(), myTextField.getText()};
            myPCS.firePropertyChange(ViewProperties.STOP_BUTTON, null, inputFields);
        });
        buttonGrid.add(stopButton);

        return buttonGrid;
    }
}
