package model;

import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileListModel extends DefaultTableModel {
    public final static String VALID_EXTENSION_REGEX = "\\.\\S+";

    @Override
    public boolean isCellEditable(final int theRow, final int theColumn) {
        return false;
    }

    public boolean validateExtension(final String theExtension) {
        return (theExtension.matches(VALID_EXTENSION_REGEX));
    }

    public boolean validateDirectory(final String theDirectory) {
        Path directoryPath = Paths.get(theDirectory);
        return Files.exists(directoryPath);
    }
}
