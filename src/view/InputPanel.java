package view;

import controller.FileListController;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeSupport;

public class InputPanel extends JPanel {
    private PropertyChangeSupport myPCS;
    private JComboBox<String> myComboBox;
    private JTextField myTextField;
    private final static JButton myStartButton = new JButton("Start");
    private final static JButton myStopButton = new JButton("Stop");
    private final static JTable myJTable = FileListController.getFileListTable();
    private final static JFileChooser myJFileChooser = new JFileChooser();

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

        JPanel fileList = new FileListPanel();
        fileList.add(initStopButton(), BorderLayout.SOUTH);
        add(fileList, BorderLayout.SOUTH);
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
        directoryPanel.add(initInputButtons(), BorderLayout.SOUTH);

        return directoryPanel;
    }

    private JPanel initInputButtons() {
        JPanel buttonGrid = new JPanel(new GridLayout());

        myStartButton.addActionListener(theEvent -> {
            String[] inputFields = {(String) myComboBox.getSelectedItem(), myTextField.getText()};
            myPCS.firePropertyChange(ViewProperties.START_BUTTON, null, inputFields);
        });
        myStartButton.setMnemonic(KeyEvent.VK_S);
        buttonGrid.add(myStartButton);

        JButton browseButton = new JButton("Browse Files");
        browseButton.addActionListener(theEvent -> {
            setUpBrowseButton();
        });
        browseButton.setMnemonic(KeyEvent.VK_B);
        buttonGrid.add(browseButton);

        return buttonGrid;
    }

    private JButton initStopButton() {
        myJTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                /*
                check selected row so button does not re-enable when
                adding to the list again, since adding triggers valueChanged
                 */
                if (myJTable.getSelectedRow() != -1) {
                    myStopButton.setEnabled(true);
                }
            }
        });

        myStopButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.STOP_BUTTON, null, myJTable.getSelectedRow());
            myStopButton.setEnabled(false);
        });
        myStopButton.setEnabled(false);
        myStopButton.setMnemonic(KeyEvent.VK_T);
        return myStopButton;
    }

    private void setUpBrowseButton() {
        myJFileChooser.setDialogTitle("Choose Directory");
        myJFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = myJFileChooser.showDialog(this, "Select");
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            myTextField.setText(myJFileChooser.getSelectedFile().toString());
        }
    }
}
