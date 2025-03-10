package model;

import javax.annotation.Nonnull;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class FileListModel extends DefaultTableModel {
    public final static String VALID_EXTENSION_REGEX = "\\.\\S+";
    public final static String[] COLUMN_HEADER_ARRAY = {"File Extension", "Directory"};
    // public final static String VALID_DIRECTORY_REGEX = "[a-zA-Z][:][\\\\|/].+";

    public FileListModel() {
        for (String columnHeader : COLUMN_HEADER_ARRAY) {
            addColumn(columnHeader);
        }
    }

    public static boolean validateExtension(final String theExtension) {
        return (theExtension.matches(VALID_EXTENSION_REGEX));
    }

    // directory is valid if it exists, is a directory, and is not a root directory
    public static boolean validateDirectory(final String theDirectory) {
        Path directoryPath = Paths.get(theDirectory);
//        return Files.exists(directoryPath) && Files.isDirectory(directoryPath) && directoryPath.getParent() != null;
        return Files.exists(directoryPath) && Files.isDirectory(directoryPath);

    }

    public String[] popTableEntry(final int theSelectedRow) {
        String[] poppedValues = new String[2];
        poppedValues[0] = getValueAt(theSelectedRow, 0).toString();
        poppedValues[1] = getValueAt(theSelectedRow, 1).toString();
        removeRow(theSelectedRow);
        return poppedValues;
    }

    private boolean isRowDataValid(final String theExtension, final String theDirectory) {
        return validateExtension(theExtension) && validateDirectory(theDirectory);
    }

    /**
     * Adds the given Array to the table if the input is a valid row. The Array must have exactly 2 elements,
     * the file extension followed by the directory.
     * @param theRowData the data of the row being added
     */
    @Override
    public void addRow(@Nonnull final Object[] theRowData) {
        if (theRowData.length == 2) {
            if (isRowDataValid(theRowData[0].toString(), theRowData[1].toString())) {
                super.addRow(theRowData);
            }
        }
    }

    /**
     * Adds the given Vector to the table if the input is a valid row. The Vector must have exactly 2 elements,
     * the file extension followed by the directory.
     * @param theRowData the data of the row being added
     */
    @Override
    public void addRow(final Vector theRowData) {
        if (theRowData.size() == 2) {
            if (isRowDataValid(theRowData.get(0).toString(), theRowData.get(1).toString())) {
                super.addRow(theRowData);
            }
        }
    }

    @Override
    public boolean isCellEditable(final int theRow, final int theColumn) {
        return false;
    }
}
