package controller;

import model.FileListModel;
import model.ModelProperties;
import view.InputErrorProperties;
import view.ViewProperties;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;

/**
 * The FileListController class manages the interaction between the file list model and the user interface.
 * It watches for changes and updates the UI when needed.
 */
public class FileListController implements PropertyChangeListener {
    private static final FileListModel myFileListModel = new FileListModel();
    private static final JTable myJTable = new JTable(myFileListModel);
    private final PropertyChangeSupport myPCS;

    /**
     * Creates a FileListController to handle file list updates.
     *
     * @param thePCS Property change support used to notify components when the file list changes.
     */
    public FileListController(final PropertyChangeSupport thePCS) {
        myPCS = thePCS;
        myPCS.addPropertyChangeListener(this);

        initTableListener();
    }

    /**
     * Sets up a listener to detect table changes and notify other components,
     * such as the UI, event listeners, or other controllers, when a file is added.
     */
    private void initTableListener() {
        myJTable.getModel().addTableModelListener(theEvent -> {
            if (theEvent.getType() == TableModelEvent.INSERT) {
                myPCS.firePropertyChange(ModelProperties.FILE_LIST_MODEL_UPDATED, null, myJTable.getModel());
            }
        });
    }

    /**
     * Handles property change events and performs actions based on user interactions.
     *
     * @param theEvent The event triggered when a user removes a file from the list.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        switch (theEvent.getPropertyName()) {
            case ViewProperties.ADD_BUTTON:
                String[] startInput = new String[] {
                        ((Map<?, ?>) theEvent.getNewValue()).get("Extension").toString(),
                        ((Map<?, ?>) theEvent.getNewValue()).get("Directory").toString(),
                        ((Map<?, ?>) theEvent.getNewValue()).get("Recursive").toString()
                };

                boolean isExtensionValid = FileListModel.validateExtension(startInput[0]);
                boolean isDirectoryValid = FileListModel.validateDirectory(startInput[1]);

                if (isExtensionValid && isDirectoryValid) {
                    myFileListModel.addRow(startInput);
                    myPCS.firePropertyChange(ViewProperties.ADDED_TO_FILE_LIST_MODEL, null, startInput);
                } else if (!isExtensionValid && !isDirectoryValid) {
                    myPCS.firePropertyChange(InputErrorProperties.BOTH_INPUTS, null, null);
                    // myMainFrame.showErrorWindow(InputErrorProperties.BOTH_INPUTS);
                } else if (!isExtensionValid) {
                    myPCS.firePropertyChange(InputErrorProperties.EXTENSION, null, null);
                    // myMainFrame.showErrorWindow(InputErrorProperties.EXTENSION);
                } else {
                    myPCS.firePropertyChange(InputErrorProperties.DIRECTORY, null, null);
                    // myMainFrame.showErrorWindow(InputErrorProperties.DIRECTORY);
                }
                break;
            case ViewProperties.REMOVE_BUTTON:
                final int selectedRow = (int) theEvent.getNewValue();
                if (selectedRow >= 0) {
                    String[] removedValues = myFileListModel.popTableEntry(selectedRow);
                    myPCS.firePropertyChange(ViewProperties.REMOVED_FROM_FILE_LIST_MODEL, null, removedValues);
                }
                break;
            default:
                break;
        }
    }
}
