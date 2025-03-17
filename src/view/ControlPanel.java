package view;

import java.awt.*;
import java.beans.PropertyChangeSupport;

import javax.swing.JPanel;

class ControlPanel extends JPanel {

    private PropertyChangeSupport myPCS;

    ControlPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        setLayout(new BorderLayout(10, 10));
        this.add(new InputPanel(myPCS), BorderLayout.NORTH);
    }
}
