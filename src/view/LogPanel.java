package view;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import model.ModelProperties;

// log under config view
class LogPanel extends JPanel implements PropertyChangeListener {
    private final PropertyChangeSupport myPCS;
    private final JTable myJTable;

    public LogPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        myPCS.addPropertyChangeListener(this);
        myJTable = new JTable(new DefaultTableModel());
        JScrollPane tableContainer = new JScrollPane(myJTable);
        myJTable.setEnabled(false);
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        if (theEvent.getPropertyName().equals(ModelProperties.LOG_LIST_MODEL_UPDATED)) {
            myJTable.setModel((TableModel) theEvent.getNewValue());
        }
    }
}
