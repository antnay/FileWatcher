package view;

import java.awt.*;
import java.beans.PropertyChangeSupport;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import model.Event;

// log under config view
class LogPanel extends JPanel {

    // private PropertyChangeSupport myPCS;
    private DefaultTableModel myTableModel;
    private JTable myLogTable;
    private JScrollPane myLogContainer;

    LogPanel(PropertyChangeSupport thePcs) {
        // myPCS = thePcs;
        myTableModel = new DefaultTableModel();
        myLogTable = new JTable(myTableModel);
        myLogContainer = new JScrollPane(myLogTable);
        myLogTable.setEnabled(false);
        setLayout(new BorderLayout());
        initTable();
        add(myLogContainer, BorderLayout.CENTER);
    }

    void addEvent(Event theEvent) {
        myTableModel.addRow(theEvent.toArray());
    }

    private void initTable() {
        myTableModel.addColumn("Extension");
        myTableModel.addColumn("File Name");
        myTableModel.addColumn("Path");
        myTableModel.addColumn("Event");
        myTableModel.addColumn("Timestamp");
    }
}
