package model;

import javax.swing.table.DefaultTableModel;

/**
 * Table model that holds events to display in the list of log events.
 */
public class LogListModel extends DefaultTableModel {
    /**
     * Constant array holding the names of the columns in the table.
     */
    public final static String[] COLUMN_HEADER_ARRAY = {"Extension", "File Name", "Path", "Event", "Timestamp"};

    /**
     * Constructs a table model with columns in the order of this class' <code>COLUMN_HEADER_ARRAY</code>.
     */
    public LogListModel() {
        for (String currentHeader : COLUMN_HEADER_ARRAY) {
            addColumn(currentHeader);
        }
    }

    /**
     * Clears the contents of the table.
     */
    public void clearTable() {
        setRowCount(0);
    }

    /**
     * Returns false regardless of parameter values to disable editing cells.
     *
     * @param theRow             the row whose value is to be queried
     * @param theColumn          the column whose value is to be queried
     * @return <code>false</code> regardless of parameter values.
     */
    @Override
    public boolean isCellEditable(final int theRow, final int theColumn) {
        return false;
    }
}
