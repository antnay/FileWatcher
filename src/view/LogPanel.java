package view;

import java.awt.*;
import java.beans.PropertyChangeSupport;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import controller.FileListController;
import model.Event;

// log under config view
class LogPanel extends JPanel {

    // private PropertyChangeSupport myPCS;
    private final JTable myFileListTableReference;
    private DefaultTableModel myTableModel;
    private JTable myLogTable;
    private JScrollPane myLogContainer;

    LogPanel(PropertyChangeSupport thePcs) {
        // myPCS = thePcs;
        myFileListTableReference = FileListController.getFileListTable();
        myTableModel = new DefaultTableModel();
        myLogTable = new JTable(myTableModel);
        myLogContainer = new JScrollPane(myLogTable);
        myLogTable.setEnabled(false);
        setLayout(new BorderLayout());
        initTable();
        add(myLogContainer, BorderLayout.CENTER);
    }

    void addEvent(Event theEvent) {
        if (isCombinationInTable(theEvent.getMyExtension(), theEvent.getPath(), myFileListTableReference)) {
            myTableModel.addRow(theEvent.toArray());
        }
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

    private void initTable() {
        myTableModel.addColumn("Extension");
        myTableModel.addColumn("File Name");
        myTableModel.addColumn("Path");
        myTableModel.addColumn("Event");
        myTableModel.addColumn("Timestamp");
    }
}
