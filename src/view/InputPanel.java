package view;

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

/**
 * An element of the GUi that lets the user enter a file extension and directory
 * to have the watcher system monitor for changes.
 */
public class InputPanel extends JPanel {
    /**
     * Constant reference to a JButton that allows users to add files and
     * extensions to the list of changes the watcher system looks for.
     */
    private final static JButton MY_ADD_BUTTON = new JButton("Add");

    /**
     * Constant reference to a JFileChooser that allows users to browse their file system
     * to select a directory instead of needing to type the path into the text field.
     */
    private final static JFileChooser MY_J_FILE_CHOOSER = new JFileChooser();

    /**
     * <code>PropertyChangeSupport</code> for notifying listeners of events.
     */
    private final PropertyChangeSupport myPCS;

    /**
     * JComboBox that allows users to type a file extension starting with a '.' character
     * or choose an extension from the dropdown list. Gets automatically filled with the extension if the user selects
     * a file in the file browser.
     */
    private JComboBox<String> myComboBox;

    /**
     * JTextField that lets users type the path to a directory on their machine. Gets automatically filled
     * if the user selects a directory in the file browser.
     */
    private JTextField myTextField;

    /**
     * JCheckBox that allows the user to enable watching directories recursively
     * (adds all subdirectories in the given directory).
     */
    private JCheckBox myRecurCheckBox ;

    /**
     * Constructs a panel that provides input fields and buttons for users to
     * enter extensions and directories for the watcher system to watch.
     *
     * @param thePcs The <code>PropertyChangeSupport</code> that this listener should be added to.
     */
    public InputPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;

        BorderLayout layout = new BorderLayout(10, 10);
        setLayout(layout);

        // label telling the user what to do
        JLabel instructionLabel = new JLabel("Select a file extension, a directory, and click Start to begin File Watcher.");
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(instructionLabel, BorderLayout.NORTH);

        // add the input fields for file extension and file directory
        JPanel subPanel = new JPanel(new BorderLayout());
        subPanel.add(createExtensionPanel(), BorderLayout.NORTH);
        subPanel.add(createRecursivePanel(), BorderLayout.SOUTH);
        add(subPanel, BorderLayout.WEST);
        add(createDirectoryPanel(), BorderLayout.EAST);
    }

    /**
     * Helper method that creates a panel allowing users to enter a file extension.
     *
     * @return The <code>JPanel</code> with the extension input field.
     */
    private JPanel createExtensionPanel() {
        // label for extension combo box
        JLabel extensionLabel = new JLabel("Monitor by extension");

        // combo box for extension
        String[] commonExtensions = {
                ".txt",
                ".exe",
                ".jar",
                ".*"
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

    /**
     * Helper method that creates a panel allowing users to watch
     * directories recursively (will watch subdirectories).
     *
     * @return The <code>JPanel</code> with the toggle to watch subdirectories.
     */
    private JPanel createRecursivePanel() {
        JLabel directoryLabel = new JLabel();
        myRecurCheckBox = new JCheckBox("Watch recursively");

        JPanel recursivePanel = new JPanel(new BorderLayout());
        recursivePanel.add(directoryLabel, BorderLayout.NORTH);
        recursivePanel.add(myRecurCheckBox);

        return recursivePanel;
    }

    /**
     * Helper method that creates a panel allowing users to enter the path
     * to a directory they want watched by the watcher system.
     *
     * @return The <code>JPanel</code> with the directory input field.
     */
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

    /**
     * Helper method that creates a panel with a button for users to submit the extension
     * and directory they have entered, as well as a button to open a file browser to select a directory or file.
     *
     * @return The <code>JPanel</code> with the add and browse buttons.
     */
    private JPanel initInputButtons() {
        JPanel buttonGrid = new JPanel(new GridLayout());

        MY_ADD_BUTTON.addActionListener(theEvent -> {
            setUpAddButton();
        });
        MY_ADD_BUTTON.setMnemonic(KeyEvent.VK_S);
        MY_ADD_BUTTON.setEnabled(false);
        buttonGrid.add(MY_ADD_BUTTON);

        JButton browseButton = new JButton("Browse Files");
        browseButton.addActionListener(theEvent -> {
            setUpBrowseButton();
        });
        browseButton.setMnemonic(KeyEvent.VK_B);
        buttonGrid.add(browseButton);

        return buttonGrid;
    }

    /**
     * Helper method that adds functionality to the add button. Gets the input
     * from the text fields and checkbox and sends that data to the controller so it can be added to the model.
     */
    private void setUpAddButton() {
        Map<String, String> userInput = new HashMap<>();
        // get input from user and trim whitespace
        userInput.put("Extension", Objects.requireNonNull(myComboBox.getSelectedItem()).toString().strip());
        userInput.put("Directory", myTextField.getText().strip());
        userInput.put("Recursive", Boolean.toString(myRecurCheckBox.isSelected()));
        myPCS.firePropertyChange(ViewProperties.ADD_BUTTON, null, userInput);
        clearInput();
    }

    /**
     * Helper method that clears the input fields and disables the add button.
     */
    private void clearInput() {
        myComboBox.setSelectedIndex(-1);
        myTextField.setText("");
        myRecurCheckBox.setSelected(false);
        MY_ADD_BUTTON.setEnabled(false);
    }

    /**
     * Helper method to add functionality to the browse button. If the user picks a directory the directory field
     * will be populated with a path to the chosen directory. If the user picks a file the extension field
     * will be populated with extension of the selected file, and the directory field will be
     * populated with the path to the directory that the selected file was located in.
     */
    private void setUpBrowseButton() {
        MY_J_FILE_CHOOSER.setDialogTitle("Choose Directory");
        MY_J_FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int returnValue = MY_J_FILE_CHOOSER.showDialog(this, "Select");
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = MY_J_FILE_CHOOSER.getSelectedFile();
            if (selectedFile.isDirectory()) {
                myTextField.setText(selectedFile.toString());
            } else if (selectedFile.isFile()) {
                String fileName = selectedFile.getName();
                myComboBox.setSelectedItem(fileName.substring(fileName.lastIndexOf('.')));
                myTextField.setText(selectedFile.getParent());
            }
        }
    }

    /**
     * Helper method that checks if the data in the extension and directory fields is
     * valid and enables or disables the add button accordingly.
     */
    private void checkForInput() {
        String extensionInput = myComboBox.getEditor().getItem().toString().strip();
        boolean extensionHasInput = extensionInput.matches(FileListModel.VALID_EXTENSION_REGEX);

        String directoryInput = myTextField.getText().strip();
        boolean directoryHasInput = new File(directoryInput).exists();

        // enable start button if extension and directory input are both not empty/null
        MY_ADD_BUTTON.setEnabled(extensionHasInput && directoryHasInput);
    }

    /**
     * Inner class to allow reuse of document listener properties for both input fields.
     */
    private class InputDocumentListener implements DocumentListener {
        /**
         * Gives a notification that something was inserted into the field.
         *
         * @param theE the document event
         */
        @Override
        public void insertUpdate(final DocumentEvent theE) {
            checkForInput();
        }

        /**
         * Gives a notification that something was removed from the field.
         *
         * @param theE the document event
         */
        @Override
        public void removeUpdate(final DocumentEvent theE) {
            checkForInput();
        }

        /**
         * Gives a notification that an attribute of the field changed.
         *
         * @param theE the document event
         */
        @Override
        public void changedUpdate(final DocumentEvent theE) {
            checkForInput();
        }
    }
}
