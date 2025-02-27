package controller;

import model.FileListModel;
import view.MainFrame;
import view.ViewProperties;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class FileListController implements PropertyChangeListener {
    private static final FileListModel myFileListModel = new FileListModel();

    public FileListController(final MainFrame theView) {
        theView.addPropertyChangeListener(this);
    }

    public static JTable getFileListModel() {
        FileListModel model = myFileListModel;
        JTable fileTable = new JTable();
        fileTable.setModel(model);

        model.addColumn("File Extension");
        model.addColumn("Directory");

        return fileTable;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        switch (theEvent.getPropertyName()) {
            case ViewProperties.START_BUTTON:
                System.out.println("start clicked");
                String[] startInput = (String[]) theEvent.getNewValue();
                myFileListModel.addRow(startInput);
                break;
            case ViewProperties.STOP_BUTTON:
                System.out.println("stop clicked");
                String[] stopInput = (String[]) theEvent.getNewValue();
                break;
            default:
                break;
        }
    }
}
