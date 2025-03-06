package controller;

import model.FileListModel;
import view.InputErrorProperties;
import view.MainFrame;
import view.ViewProperties;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

public class FileListController implements PropertyChangeListener {
    private static final FileListModel myFileListModel = new FileListModel();
    private static final JTable myJTable = new JTable(myFileListModel);
    private final MainFrame myMainFrame;

    public FileListController(final MainFrame theView) {
        myMainFrame = theView;
        myMainFrame.addPropertyChangeListener(this);
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
                String[] startInput = new String[] {
                        ((Map<?, ?>) theEvent.getNewValue()).get("Extension").toString(),
                        ((Map<?, ?>) theEvent.getNewValue()).get("Directory").toString()
                };

                boolean isExtensionValid = myFileListModel.validateExtension(startInput[0]);
                boolean isDirectoryValid = myFileListModel.validateDirectory(startInput[1]);

                if (isExtensionValid && isDirectoryValid) {
                    myFileListModel.addRow(startInput);
                } else if (!isExtensionValid && !isDirectoryValid) {
                    myMainFrame.showErrorWindow(InputErrorProperties.BOTH_INPUTS);
                } else if (!isExtensionValid) {
                    myMainFrame.showErrorWindow(InputErrorProperties.EXTENSION);
                } else {
                    myMainFrame.showErrorWindow(InputErrorProperties.DIRECTORY);
                }
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
