package view;

import java.awt.*;
import java.beans.PropertyChangeSupport;

import javax.swing.JButton;
import javax.swing.JPanel;

// set file extensions and directories
class ControlPanel extends JPanel {

    private PropertyChangeSupport myPCS;
    private JButton myStartStopButton;
    private JButton mySubmitButton;
    private JButton myClearButton; // temp
    private JButton mySaveLogButton;

    ControlPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        setLayout(new BorderLayout(10, 10));
        this.add(new InputPanel(myPCS), BorderLayout.NORTH);
        this.add(new FileListPanel(myPCS), BorderLayout.CENTER);
    }

    void updateStartStopButt(boolean theIsRunning) {
        myStartStopButton.setText((theIsRunning) ? "Stop" : "Start");
    }
}
