package view;

import java.awt.*;
import java.beans.PropertyChangeSupport;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * The ControlPanel class creates the main panel for user input.
 * It arranges the layout and links to other UI components.
 */
// set file extensions and directories
class ControlPanel extends JPanel {

    private PropertyChangeSupport myPCS;
//    private JButton myStartStopButton;
//    private JButton mySubmitButton;
//    private JButton myClearButton; // temp
//    private JButton mySaveLogButton;

    /**
     * Creates the control panel and sets up the input section.
     *
     * @param thePcs Handles communication between components when changes occur.
     */
    ControlPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        setLayout(new BorderLayout(10, 10));
        this.add(new InputPanel(myPCS), BorderLayout.NORTH);
    }
}
