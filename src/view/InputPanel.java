package view;

import controller.FileListController;
import model.FileListModel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InputPanel extends JPanel {
    private final static JButton myStartButton = new JButton("Add");
    private final static JFileChooser myJFileChooser = new JFileChooser();
    private final PropertyChangeSupport myPCS;
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
        // set the combo box to be empty by default
        myComboBox.setSelectedIndex(-1);

        JTextComponent editorComponent = (JTextComponent) myComboBox.getEditor().getEditorComponent();
        editorComponent.getDocument().addDocumentListener(new InputDocumentListener());

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

        // check input when content of text field changes
        myTextField.getDocument().addDocumentListener(new InputDocumentListener());

        JPanel directoryPanel = new JPanel(new BorderLayout());
        directoryPanel.add(directoryLabel, BorderLayout.NORTH);
        directoryPanel.add(myTextField, BorderLayout.CENTER);
        directoryPanel.add(initInputButtons(), BorderLayout.SOUTH);

        return directoryPanel;
    }

    private JPanel initInputButtons() {
        JPanel buttonGrid = new JPanel(new GridLayout());

        myStartButton.addActionListener(theEvent -> {
            setUpStartButton();
        });
        myStartButton.setMnemonic(KeyEvent.VK_S);
        myStartButton.setEnabled(false);
        buttonGrid.add(myStartButton);

        JButton browseButton = new JButton("Browse Files");
        browseButton.addActionListener(theEvent -> {
            setUpBrowseButton();
        });
        browseButton.setMnemonic(KeyEvent.VK_B);
        buttonGrid.add(browseButton);

        return buttonGrid;
    }

    private void setUpBrowseButton() {
        myJFileChooser.setDialogTitle("Choose Directory");
        myJFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int returnValue = myJFileChooser.showDialog(this, "Select");
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = myJFileChooser.getSelectedFile();
            if (selectedFile.isDirectory()) {
                myTextField.setText(selectedFile.toString());
            } else if (selectedFile.isFile()) {
                String fileName = selectedFile.getName();
                myComboBox.setSelectedItem(fileName.substring(fileName.lastIndexOf('.')));
                myTextField.setText(selectedFile.getParent());
            }
        }
    }

    private void setUpStartButton() {
        Map<String, String> userInput = new HashMap<>();
        // get input from user and trim whitespace
        userInput.put("Extension", Objects.requireNonNull(myComboBox.getSelectedItem()).toString().strip());
        userInput.put("Directory", myTextField.getText().strip());
        myPCS.firePropertyChange(ViewProperties.START_BUTTON, null, userInput);
    }

    private void clearInput() {
        myComboBox.setSelectedIndex(-1);
        myTextField.setText("");
        myStartButton.setEnabled(false);
    }

    private void checkForInput() {
        String extensionInput = myComboBox.getEditor().getItem().toString().strip();
        boolean extensionHasInput = extensionInput.matches(FileListModel.VALID_EXTENSION_REGEX);

        String directoryInput = myTextField.getText().strip();
        // boolean directoryHasInput = directoryInput.matches(FileListModel.VALID_DIRECTORY_REGEX);
        boolean directoryHasInput = new File(directoryInput).exists();

        // enable start button if extension and directory input are both not empty/null
        myStartButton.setEnabled(extensionHasInput && directoryHasInput);
    }

    // inner class to reuse document listener properties for both input fields
    private class InputDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            checkForInput();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            checkForInput();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            checkForInput();
        }
    }
}
