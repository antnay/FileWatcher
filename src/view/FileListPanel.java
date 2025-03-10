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

public class FileListPanel extends JPanel implements PropertyChangeListener {
    private final JTable myJTable;
    private final static JButton myStopButton = new JButton("Remove");

    public FileListPanel(final PropertyChangeSupport thePcs) {
        String[][] empty2DStringArray = new String[0][0];
        myJTable = new JTable(empty2DStringArray, FileListModel.COLUMN_HEADER_ARRAY);
        myJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        thePcs.addPropertyChangeListener(this);
        setLayout(new BorderLayout());
        JLabel fileListLabel = new JLabel("Files and directories being watched");
        add(fileListLabel, BorderLayout.NORTH);
        initFileList();
        initStopButton();

        add(initStopButton(), BorderLayout.SOUTH);
    }

    private void initFileList() {
        JScrollPane listScrollPane = new JScrollPane(myJTable);
        listScrollPane.setPreferredSize(new Dimension(400, 100));
        add(listScrollPane, BorderLayout.CENTER);
    }

    private JButton initStopButton() {
        myJTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                /*
                check selected row so button does not re-enable when
                adding to the list again, since adding triggers valueChanged
                 */
                if (myJTable.getSelectedRow() != -1) {
                    myStopButton.setEnabled(true);
                }
            }
        });

        myStopButton.addActionListener(theEvent -> {
            //myPCS.firePropertyChange(ViewProperties.STOP_BUTTON, null, myJTable.getSelectedRow());
            myStopButton.setEnabled(false);
        });
        myStopButton.setEnabled(false);
        myStopButton.setMnemonic(KeyEvent.VK_T);
        return myStopButton;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        if (theEvent.getPropertyName().equals(ModelProperties.FILE_LIST_MODEL_UPDATED)) {
            myJTable.setModel((TableModel) theEvent.getNewValue());
        }
    }
}
