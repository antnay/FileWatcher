package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class LogListModelTest {

    private LogListModel logListModel;
    private Object[] validRow;
    private Vector<String> validRowVector;

    @BeforeEach
    void setUp() {
        logListModel = new LogListModel();
        validRow = new Object[]{".txt", "example.txt", "/user/home", "Created", "2025-03-20 12:00:00"};
        validRowVector = new Vector<>();
        validRowVector.add(".txt");
        validRowVector.add("example.txt");
        validRowVector.add("/user/home");
        validRowVector.add("Created");
        validRowVector.add("2025-03-20 12:00:00");
    }

    @Test
    void clearTable() {
        logListModel.addRow(validRow);
        assertEquals(1, logListModel.getRowCount());

        logListModel.clearTable();
        assertEquals(0, logListModel.getRowCount());
    }

    @Test
    void addRow() {
        assertEquals(0, logListModel.getRowCount());

        logListModel.addRow(validRow);
        assertEquals(1, logListModel.getRowCount());
    }

    @Test
    void testAddRow() {
        assertEquals(0, logListModel.getRowCount());

        logListModel.addRow(validRowVector);
        assertEquals(1, logListModel.getRowCount());
    }

    @Test
    void isCellEditable() {
        assertFalse(logListModel.isCellEditable(0, 0));
        assertFalse(logListModel.isCellEditable(1, 1));
    }
}