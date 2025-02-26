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
        initFileList();
    }

    private void initFileList() {
        DefaultTableModel model = new DefaultTableModel();
        JTable fileTable = new JTable(model);

        model.addColumn("File Extension");
        model.addColumn("Directory");
        JScrollPane listScrollPane = new JScrollPane(fileTable);
        // set File Extension column width
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        // set Directory column width
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        add(listScrollPane);
    }
}
