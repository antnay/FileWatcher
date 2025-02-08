package view;

import java.beans.PropertyChangeSupport;

import javax.swing.JButton;
import javax.swing.JPanel;

// set file extensions and directories
class ControlPanel extends JPanel {

    private PropertyChangeSupport myPCS;
    private JButton myStartStopButton;
    private JButton mySubmitButton;

    ControlPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        initButtons();
    }

    private void initButtons() {
        myStartStopButton = new JButton("Start");
        myStartStopButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.START_STOP_BUTTON, null, null);
        });
        add(myStartStopButton);
        mySubmitButton = new JButton("Submit");
        mySubmitButton.addActionListener(theEvent -> {
            myPCS.firePropertyChange(ViewProperties.SUBMIT_BUTTON, null, null);
        });
        add(mySubmitButton);
    }
}
