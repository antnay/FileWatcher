package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.beans.PropertyChangeSupport;

public class FileListPanel extends JPanel {
    private PropertyChangeSupport myPCS;

    public FileListPanel(PropertyChangeSupport thePcs) {
        myPCS = thePcs;
        setLayout(new BorderLayout());
        JLabel fileListLabel = new JLabel("Files and directories being watched");
        add(fileListLabel, BorderLayout.NORTH);
        initFileList();
    }

    private void initFileList() {
        DefaultTableModel model = new DefaultTableModel();
        JTable fileTable = new JTable() {
            // prevent cells in table from being editable while still being able to select rows
            @Override
            public boolean isCellEditable(int theRow, int theColumn) {
                return false;
            }
        };
        fileTable.setModel(model);

        model.addColumn("File Extension");
        model.addColumn("Directory");
        JScrollPane listScrollPane = new JScrollPane(fileTable);
        listScrollPane.setPreferredSize(new Dimension(400, 100));
        // set File Extension column width
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        // set Directory column width
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(400);

        // test data
//        for (int i = 0; i < 10; i++) {
//            String[] rowData = {".txt", String.valueOf(i)};
//            model.addRow(rowData);
//        }
        add(listScrollPane, BorderLayout.CENTER);
    }
}
