package model;

import javax.annotation.Nonnull;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class LogListModel extends DefaultTableModel {
    public final static String[] COLUMN_HEADER_ARRAY = {"Extension", "File Name", "Path", "Event", "Timestamp"};

    public LogListModel() {
        for (String currentHeader : COLUMN_HEADER_ARRAY) {
            addColumn(currentHeader);
        }
    }

    /**
     * Adds the given Array to the table if the input is a valid row. The Array must have exactly 2 elements,
     * the file extension followed by the directory.
     * @param theRowData the data of the row being added
     */
    @Override
    public void addRow(@Nonnull final Object[] theRowData) {
        super.addRow(theRowData);
    }

    /**
     * Adds the given Vector to the table if the input is a valid row. The Vector must have exactly 2 elements,
     * the file extension followed by the directory.
     * @param theRowData the data of the row being added
     */
    @Override
    public void addRow(final Vector theRowData) {
        super.addRow(theRowData);
    }


    @Override
    public boolean isCellEditable(final int theRow, final int theColumn) {
        return false;
    }
}
