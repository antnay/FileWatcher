package model;

import org.checkerframework.checker.nullness.qual.NonNull;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class LogListModel extends DefaultTableModel {
    public final static String[] COLUMN_HEADER_ARRAY = {"Extension", "File Name", "Path", "Event", "Timestamp"};

    public LogListModel() {
        for (String currentHeader : COLUMN_HEADER_ARRAY) {
            addColumn(currentHeader);
        }
    }

    public void clearTable() {
        setRowCount(0);
    }

    @Override
    public boolean isCellEditable(final int theRow, final int theColumn) {
        return false;
    }
}
