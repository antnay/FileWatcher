package controller;

import model.FileListModel;
import view.MainFrame;
import view.ViewProperties;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class FileListController implements PropertyChangeListener {
    private static final FileListModel myFileListModel = new FileListModel();
    private static final JTable myJTable = new JTable(myFileListModel);

    public FileListController(final MainFrame theView) {
        theView.addPropertyChangeListener(this);
        myFileListModel.addColumn("File Extension");
        myFileListModel.addColumn("Directory");
        myJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set File Extension column width
        myJTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        // set Directory column width
        myJTable.getColumnModel().getColumn(1).setPreferredWidth(400);
    }

    public static JTable getFileListTable() {
        return myJTable;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        switch (theEvent.getPropertyName()) {
            case ViewProperties.START_BUTTON:
                String[] startInput = (String[]) theEvent.getNewValue();
                myFileListModel.addRow(startInput);
                break;
            case ViewProperties.STOP_BUTTON:
                final int selectedRow = (int) theEvent.getNewValue();
                if (selectedRow >= 0) {
                    myFileListModel.removeRow(selectedRow);
                }
                break;
            default:
                break;
        }
    }
}
