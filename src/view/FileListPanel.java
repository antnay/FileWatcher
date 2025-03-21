package view;

import model.FileListModel;
import model.ModelProperties;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class FileListPanel extends JPanel implements PropertyChangeListener {
    private static final int SMALL_COLUMN_WIDTH = 95;
    private final JTable myJTable;
    private final static JButton myRemoveButton = new JButton("Remove");
    private final PropertyChangeSupport myPcs;

    public FileListPanel(final PropertyChangeSupport thePcs) {
        String[][] empty2DStringArray = new String[0][0];
        myJTable = new JTable(empty2DStringArray, FileListModel.COLUMN_HEADER_ARRAY);
        myJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // sets width of last column to be smaller
        myJTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        myJTable.getColumnModel().getColumn(2).setMaxWidth(SMALL_COLUMN_WIDTH);

        myPcs = thePcs;
        myPcs.addPropertyChangeListener(this);

        setLayout(new BorderLayout());
        JLabel fileListLabel = new JLabel("Files and directories being watched");
        add(fileListLabel, BorderLayout.NORTH);
        initFileList();
        initRemoveButton();

        add(initRemoveButton(), BorderLayout.SOUTH);
    }

    private void initFileList() {
        JScrollPane listScrollPane = new JScrollPane(myJTable);
        listScrollPane.setPreferredSize(new Dimension(400, 100));
        add(listScrollPane, BorderLayout.CENTER);
    }

    private JButton initRemoveButton() {
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
        return myRemoveButton;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        if (theEvent.getPropertyName().equals(ModelProperties.FILE_LIST_MODEL_UPDATED)) {
            myJTable.setModel((TableModel) theEvent.getNewValue());

            // maintain the size of the columns when getting the new model
            myJTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            myJTable.getColumnModel().getColumn(2).setMaxWidth(SMALL_COLUMN_WIDTH);
        }
    }
}
