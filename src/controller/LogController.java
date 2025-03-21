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

/**
 * The LogController keeps track of events, listens for updates, and adds new events to the log table.
 */
public class LogController implements PropertyChangeListener {
    private static final LogListModel myLogListModel = new LogListModel();
    private static final JTable myJTable = new JTable(myLogListModel);
    private final PropertyChangeSupport myPCS;

    /**
     * Sets up the log controller and listens for updates.
     *
     * @param thePCS Notifies the UI and other system components when the log changes.
     */
    public LogController(final PropertyChangeSupport thePCS) {
        myPCS = thePCS;
        myPCS.addPropertyChangeListener(this);

        initTableListener();
    }

    /**
     * Tracks changes in the log table and informs other components when updates occur.
     */
    private void initTableListener() {
        myJTable.getModel().addTableModelListener(theEvent -> {
            if (theEvent.getType() == TableModelEvent.INSERT) {
                myPCS.firePropertyChange(ModelProperties.LOG_LIST_MODEL_UPDATED, null, myJTable.getModel());
            }
        });
    }

    /**
     * Checks if a file extension and directory combination exists in the table.
     *
     * @param theExtension The file extension to look for (e.g., ".txt").
     * @param theDirectory The directory path where the file is located.
     * @param theTable The table that stores logged file events.
     * @return True if the extension and directory combination exists, otherwise false.
     */
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

    /**
     * Updates the log table when new events are added or clears it when needed.
     *
     * @param theEvent The event containing details about the change.
     */
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