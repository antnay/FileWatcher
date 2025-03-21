package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileListModelTest {

    private FileListModel myFileListModel;
    private String[] validRow;
    private String[] invalidRow;

    @BeforeEach
    void setUp() {
        myFileListModel = new FileListModel();
        validRow = new String[]{".txt", System.getProperty("user.home"), "true"};
        invalidRow = new String[]{"txt", "invalid/path", "true"};
    }

    @Test
    void validateExtension() {
        assertTrue(FileListModel.validateExtension(".txt"));
        assertFalse(FileListModel.validateExtension("txt"));
        assertFalse(FileListModel.validateExtension(" "));
        assertFalse(FileListModel.validateExtension(""));
    }

    @Test
    void validateDirectory() {
        assertFalse(FileListModel.validateDirectory("invalid/path"));
        assertTrue(FileListModel.validateDirectory(System.getProperty("user.home")));
    }

    @Test
    void popTableEntry() {
        assertEquals(0, myFileListModel.getRowCount());

        myFileListModel.addRow(validRow);
        assertEquals(1, myFileListModel.getRowCount());

        String[] removedEntry = myFileListModel.popTableEntry(0);
        assertNotNull(removedEntry);
        assertEquals(3, removedEntry.length);
        assertEquals(".txt", removedEntry[0]);
        assertEquals(System.getProperty("user.home"), removedEntry[1]);
        assertEquals("true", removedEntry[2]);

        assertEquals(0, myFileListModel.getRowCount());
    }

    @Test
    void addRow() {
        assertEquals(0, myFileListModel.getRowCount());

        myFileListModel.addRow(validRow);
        assertEquals(1, myFileListModel.getRowCount());
    }

    @Test
    void testAddRow() {
        assertEquals(0, myFileListModel.getRowCount());

        myFileListModel.addRow(invalidRow);
        assertEquals(0, myFileListModel.getRowCount());
    }

    @Test
    void isCellEditable() {
        assertFalse(myFileListModel.isCellEditable(0, 0));
        assertFalse(myFileListModel.isCellEditable(1, 1));
    }
}