package controller;

import model.Event;
import model.LogListModel;
import model.ModelProperties;
import view.ViewProperties;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class LogController implements PropertyChangeListener {
    private static final LogListModel myLogListModel = new LogListModel();
    private static final JTable myJTable = new JTable(myLogListModel);
    private final PropertyChangeSupport myPCS;

    public LogController(final PropertyChangeSupport thePCS) {
        myPCS = thePCS;
        myPCS.addPropertyChangeListener(this);

        initTableListener();
    }

    private void initTableListener() {
        myJTable.getModel().addTableModelListener(theEvent -> {
            if (theEvent.getType() == TableModelEvent.INSERT) {
                myPCS.firePropertyChange(ModelProperties.LOG_LIST_MODEL_UPDATED, null, myJTable.getModel());
            }
        });
    }

    public boolean isCombinationInTable(final String theExtension, final String theDirectory, final JTable theTable) {
        boolean comboIsInTable = false;
        for (int i = 0; i < theTable.getRowCount(); i++) {
            String currentExtension = theTable.getValueAt(i, 0).toString();
            String currentDirectory = theTable.getValueAt(i, 1).toString();
            if (currentExtension.equals(theExtension) && currentDirectory.equals(theDirectory)) {
                comboIsInTable = true;
            }
        }
        return comboIsInTable;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        switch (theEvent.getPropertyName()) {
            case ModelProperties.EVENT:
                Event eventDetails = (Event) theEvent.getNewValue();
                myLogListModel.addRow(eventDetails.toArray());
                break;
            case ViewProperties.SAVE_LOG, ViewProperties.CLEAR_LOG:
                myLogListModel.clearTable();
                break;
            default:
                break;
        }
    }
}