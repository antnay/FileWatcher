package model;

import javax.annotation.Nonnull;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class FileListModel extends DefaultTableModel {
    public final static String VALID_EXTENSION_REGEX = "\\.\\S+";
    // public final static String VALID_DIRECTORY_REGEX = "[a-zA-Z][:][\\\\|/].+";

    public FileListModel() {
        addColumn("File Extension");
        addColumn("Directory");
    }

    public boolean validateExtension(final String theExtension) {
        return (theExtension.matches(VALID_EXTENSION_REGEX));
    }

    // directory is valid if it exists, is a directory, and is not a root directory
    public boolean validateDirectory(final String theDirectory) {
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

    /**
     * Adds the given row to the table if the input is valid.
     * @param theRowData the data of the row being added
     */
    @Override
    public void addRow(@Nonnull final Object[] theRowData) {
        String[] startInput = (String[]) theRowData;
        boolean isExtensionValid = false;
        boolean isDirectoryValid = false;

        try {
            isExtensionValid = validateExtension(startInput[0]);
            isDirectoryValid = validateDirectory(startInput[1]);
        } catch (ArrayIndexOutOfBoundsException theException) {
            System.err.println("Unexpected row data in FileListModel.addRow()");
        }

        if (isExtensionValid && isDirectoryValid) {
            super.addRow(theRowData);
        }
    }

    @Override
    public void addRow(final Vector theRowData) {
        System.out.println("FileListModel.addRow(Vector is not implemented. Please use addRow(Object[])");
    }

    @Override
    public boolean isCellEditable(final int theRow, final int theColumn) {
        return false;
    }
}
