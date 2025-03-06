package model;

import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileListModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(final int theRow, final int theColumn) {
        return false;
    }

    public boolean validateExtension(final String theExtension) {
        return (theExtension.startsWith("."));
    }

    public boolean validateDirectory(final String theDirectory) {
        Path directoryPath = Paths.get(theDirectory);
        return Files.exists(directoryPath);
    }
}
