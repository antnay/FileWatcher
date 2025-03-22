package model;

import org.checkerframework.checker.nullness.qual.NonNull;

import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

/**
 * Table model that holds extensions and directories to display in the list files being watched.
 */
public class FileListModel extends DefaultTableModel {
    /**
     * Constant string of the regular expression for valid extension input.
     */
    public final static String VALID_EXTENSION_REGEX = "^[\\.][^\\s\\.]+$";

    /**
     * Constant array holding the names of the columns in the table.
     */
    public final static String[] COLUMN_HEADER_ARRAY = {"File Extension", "Directory", "Recursive"};
    // public final static String VALID_DIRECTORY_REGEX = "[a-zA-Z][:][\\\\|/].+";

    /**
     * Constructs a table model with columns in the order of this class' <code>COLUMN_HEADER_ARRAY</code>.
     */
    public FileListModel() {
        for (String columnHeader : COLUMN_HEADER_ARRAY) {
            addColumn(columnHeader);
        }
    }

    /**
     * Checks that the given String is a valid extension.
     *
     * @param theExtension The extension to validate.
     * @return <code>true</code> if the String fits <code>VALID_EXTENSION_REGEX</code>, <code>false</code> otherwise.
     */
    public static boolean validateExtension(final String theExtension) {
        return (theExtension.matches(VALID_EXTENSION_REGEX));
    }

    /**
     * Checks that the given String is a valid directory.
     *
     * @param theDirectory The directory path to validate.
     * @return <code>true</code> if the String is a path to a directory that exists, <code>false</code> otherwise.
     */
    public static boolean validateDirectory(final String theDirectory) {
        Path directoryPath = Paths.get(theDirectory);
        return Files.exists(directoryPath) && Files.isDirectory(directoryPath);

    }

    /**
     * Removes a row from the table and returns the removed row's contents as an array.
     *
     * @param theSelectedRow The row to remove from the table.
     * @return The contents of the removed row as an array, with the
     * data being in the order of <code>COLUMN_HEADER_ARRAY</code>
     */
    public String[] popTableEntry(final int theSelectedRow) {
        String[] poppedValues = new String[3];
        poppedValues[0] = getValueAt(theSelectedRow, 0).toString();
        poppedValues[1] = getValueAt(theSelectedRow, 1).toString();
        poppedValues[2] = getValueAt(theSelectedRow, 2).toString();
        removeRow(theSelectedRow);
        return poppedValues;
    }

    /**
     * Helper method to combine validating validateExtension and validateDirectory.
     *
     * @param theExtension The extension to validate.
     * @param theDirectory The directory path to validate.
     * @return <code>true</code> if the extension and directory are
     * valid in validateExtension and validateDirectory, <code>false</code> otherwise.
     */
    private boolean isRowDataValid(final String theExtension, final String theDirectory) {
        return validateExtension(theExtension) && validateDirectory(theDirectory);
    }

    /**
     * Adds the given Array to the table if the input is a valid row. The Array must have exactly 3 elements,
     * in the order of <code>COLUMN_HEADER_ARRAY</code>.
     *
     * @param theRowData the data of the row being added
     */
    @Override
    public void addRow(@NonNull final Object[] theRowData) {
        if (theRowData.length == 3) {
            if (isRowDataValid(theRowData[0].toString(), theRowData[1].toString())) {
                super.addRow(theRowData);
            }
        }
    }

    /**
     * Adds the given Vector to the table if the input is a valid row. The Vector must have exactly 3 elements,
     * in the order of <code>COLUMN_HEADER_ARRAY</code>.
     *
     * @param theRowData the data of the row being added
     */
    @Override
    public void addRow(final Vector theRowData) {
        if (theRowData.size() == 3) {
            if (isRowDataValid(theRowData.get(0).toString(), theRowData.get(1).toString())) {
                super.addRow(theRowData);
            }
        }
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
