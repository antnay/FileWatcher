package controller;

import model.Event;
import model.LogListModel;
import model.ModelProperties;
import view.MainFrame;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class LogController implements PropertyChangeListener {
    private static final LogListModel myLogListModel = new LogListModel();
    private static final JTable myJTable = new JTable();
    private final MainFrame myMainFrame;
    private final PropertyChangeSupport myPCS;

    public LogController(final MainFrame theMainFrame) {
        myMainFrame = theMainFrame;
        myPCS = myMainFrame.getPCS();
        myMainFrame.addPropertyChangeListener(this);

        initTableListener();
    }

    private void initTableListener() {
        myJTable.getModel().addTableModelListener(theEvent -> {
            if (theEvent.getType() == TableModelEvent.INSERT) {
                myPCS.firePropertyChange(ModelProperties.FILE_LIST_MODEL_UPDATED, null, myJTable.getModel());
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

//    private void addEvent(Event theEvent) {
//        if (isCombinationInTable(theEvent.getMyExtension(), theEvent.getPath(), myFileListTableReference)) {
//            myTableModel.addRow(theEvent.toArray());
//        }
//    }

    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        if (theEvent.getPropertyName().equals(ModelProperties.EVENT)) {
            System.out.println("Event was received in LogController:" + theEvent.getNewValue().toString());
        }
    }
}