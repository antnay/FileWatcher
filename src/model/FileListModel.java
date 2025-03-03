package model;

import javax.swing.table.DefaultTableModel;

public class FileListModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(final int theRow, final int theColumn) {
        return false;
    }
}
