package view;

import java.awt.*;
import java.beans.PropertyChangeSupport;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

// set file extensions and directories
class ControlPanel extends JPanel {

    private PropertyChangeSupport myPCS;
    private JButton myStartStopButton;
    private JButton mySubmitButton;
    private JButton myClearButton; // temp
    private JButton mySaveLogButton;
    private JLabel myExtensionLabel;
    private JTextField myFileExtension;
    private JLabel myDirectoryLabel;
    private JTextField myDirectory;
    private final GridBagConstraints gridConstraints = new GridBagConstraints();

    ControlPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        setLayout(new GridBagLayout());
        initTextFields();
        initButtons();
    }

    void updateStartStopButt(boolean theIsRunning) {
        myStartStopButton.setText((theIsRunning) ? "Stop" : "Start");
    }

    private void initTextFields() {
        JLabel instructionLabel = new JLabel("Select a file extension, a directory, and click Start to begin File Watcher.");

        add(instructionLabel);
    }

    private void initButtons() {
        myStartStopButton = new JButton("Start");
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 1;
        myStartStopButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.START_STOP_BUTTON, null, null);
        });
        add(myStartStopButton, gridConstraints);

        mySubmitButton = new JButton("Submit");
        gridConstraints.gridx = 2;
        gridConstraints.gridy = 1;
        mySubmitButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.SUBMIT_BUTTON, null, null);
        });
        add(mySubmitButton, gridConstraints);

        myClearButton = new JButton("Clear");
        gridConstraints.gridx = 3;
        gridConstraints.gridy = 1;
        myClearButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.CLEAR_LOG_BUTTON, null, null);
        });
        add(myClearButton, gridConstraints);

        mySaveLogButton = new JButton("Save");
        gridConstraints.gridx = 4;
        gridConstraints.gridy = 1;
        mySaveLogButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
        });
        add(mySaveLogButton, gridConstraints);
    }
}
