package view;

import model.FileListModel;
import model.ModelProperties;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * An element of the GUI that shows the user a list of file extensions and directories that are being watched.
 */
public class FileListPanel extends JPanel implements PropertyChangeListener {
    /**
     * Constant int for how wide the last column in the table should be.
     */
    private final static int LAST_COLUMN_WIDTH = 95;

    /**
     * Constant reference to a JButton that allows the user to remove the selected extension
     * and directory from the watcher system so changes will no longer be tracked.
     */
    private final static JButton myRemoveButton = new JButton("Remove");

    /**
     * The JTable displayed in this panel.
     */
    private final JTable myJTable;

    /**
     * <code>PropertyChangeSupport</code> for notifying listeners of events.
     */
    private final PropertyChangeSupport myPcs;

    /**
     * Creates a panel that displays a list of files and extensions that the user entered
     * to be tracked by the watcher system. Allows the user to select rows in the table and remove them if desired.
     *
     * @param thePcs The <code>PropertyChangeSupport</code> that this listener should be added to.
     */
    public FileListPanel(final PropertyChangeSupport thePcs) {
        String[][] empty2DStringArray = new String[0][0];
        myJTable = new JTable(empty2DStringArray, FileListModel.COLUMN_HEADER_ARRAY);
        myJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // sets width of last column to be smaller
        myJTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        myJTable.getColumnModel().getColumn(2).setMaxWidth(LAST_COLUMN_WIDTH);

        myPcs = thePcs;
        myPcs.addPropertyChangeListener(this);

        setLayout(new BorderLayout());
        JLabel fileListLabel = new JLabel("Files and directories being watched");
        add(fileListLabel, BorderLayout.NORTH);

        // display table
        JScrollPane listScrollPane = new JScrollPane(myJTable);
        listScrollPane.setPreferredSize(new Dimension(400, 100));
        add(listScrollPane, BorderLayout.CENTER);

        initRemoveButton();

        add(myRemoveButton, BorderLayout.SOUTH);
    }

    /**
     * Helper method that adds functionality to the remove button. Gets the selected row from the table
     * sends that data to the controller so it can be removed from the model.
     */
    private void initRemoveButton() {
        myJTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                /*
                check selected row so button does not re-enable when
                adding to the list again, since adding triggers valueChanged
                 */
                if (myJTable.getSelectedRow() != -1) {
                    myRemoveButton.setEnabled(true);
                }
            }
        });

        myRemoveButton.addActionListener(theEvent -> {
            myPcs.firePropertyChange(ViewProperties.REMOVE_BUTTON, null, myJTable.getSelectedRow());
            myRemoveButton.setEnabled(false);
        });
        myRemoveButton.setEnabled(false);
        myRemoveButton.setMnemonic(KeyEvent.VK_T);
    }

    /**
     * This method gets called when a property bound in a
     * <code>PropertyChangeSupport</code> this listener is added to is changed.
     *
     * @param theEvent A PropertyChangeEvent object describing the event source
     *          and the property that has changed.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        if (theEvent.getPropertyName().equals(ModelProperties.FILE_LIST_MODEL_UPDATED)) {
            myJTable.setModel((TableModel) theEvent.getNewValue());

            // maintain the size of the columns when getting the new model
            myJTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            myJTable.getColumnModel().getColumn(2).setMaxWidth(LAST_COLUMN_WIDTH);
        }
    }
}
