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

    ControlPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        setLayout(new GridBagLayout());
        initInputFields();
        initButtons();
    }

    void updateStartStopButt(boolean theIsRunning) {
        myStartStopButton.setText((theIsRunning) ? "Stop" : "Start");
    }

    private void initInputFields() {
        // label telling the user what to do
        JLabel instructionLabel = new JLabel("Select a file extension, a directory, and click Start to begin File Watcher.");
        instructionLabel.setBackground(Color.RED);
        instructionLabel.setOpaque(true);
        add(instructionLabel);

        // label for extension field
        JLabel extensionLabel = new JLabel("Monitor by extension");
        add(extensionLabel);

        // label for directory field
        JLabel directoryLabel = new JLabel("Directory to monitor");
        add(directoryLabel);

        // text field for extension
        JTextField extensionField = new JTextField();
        add(extensionField);

        // text field for directory
        JTextField directoryField = new JTextField();
        add(directoryField);

        // start and stop buttons
    }

    private void initButtons() {
        myStartStopButton = new JButton("Start");
        myStartStopButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.START_STOP_BUTTON, null, null);
        });
        //add(myStartStopButton);

        mySubmitButton = new JButton("Submit");
        mySubmitButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.SUBMIT_BUTTON, null, null);
        });
        //add(mySubmitButton);

        myClearButton = new JButton("Clear");
        myClearButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.CLEAR_LOG_BUTTON, null, null);
        });
        //add(myClearButton);

        mySaveLogButton = new JButton("Save");
        mySaveLogButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
        });
        //add(mySaveLogButton);
    }
}
