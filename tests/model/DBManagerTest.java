package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class DBManagerTest {

    private DBManager myDBManager;

    @BeforeEach
    void setUp() {
        myDBManager = DBManager.getDBManager();
        try {
            myDBManager.connect();
        } catch (DatabaseException e) {
            fail("Failed to connect to database: " + e.getMessage());
        }
    }

    @Test
    void isConnected() {
        assertTrue(myDBManager.isConnected(), "Database connected.");
    }

    @Test
    void connect() {
        assertDoesNotThrow(() -> myDBManager.connect(), "Database should connect without errors.");
    }

    @Test
    void disconnect() {
        assertDoesNotThrow(() -> myDBManager.disconnect(), "Database should disconnect without errors.");
    }

    @Test
    void executeQuery() {
        assertDoesNotThrow(() -> {
            ResultSet resultSet = myDBManager.executeQuery("SELECT 1;");
            assertNotNull(resultSet, "Query result should not be null.");
        }, "Executing a query should not fail.");
    }

    @Test
    void clearTable() {
        assertDoesNotThrow(() -> myDBManager.clearTable(), "Event log should clear without errors..");
    }

    @Test
    void clearTempTable() {
        assertDoesNotThrow(() -> myDBManager.clearTempTable(), "Temporary event log should clear without errors.");
    }

    @Test
    void clearWatchTable() {
        assertDoesNotThrow(() -> myDBManager.clearWatchTable(), "Watch table should clear without errors.");
    }

    @Test
    void addToWatch() {
        File testFile = new File("/test/path/testFile.txt");
        assertDoesNotThrow(() -> myDBManager.addToWatch(testFile), "Adding a file to the watch table should not throw an error.");
    }

    @Test
    void addEvent() {
        Event testEvent = new Event("txt", "testFile.txt", "/test/path", "CREATE");
        assertDoesNotThrow(() -> myDBManager.addEvent(testEvent), "Adding an event should not throw an error.");
    }

    @Test
    void mergeTempEvents() {
        assertDoesNotThrow(() -> myDBManager.mergeTempEvents(), "Merging temp events should not throw an error.");
    }

    @Test
    void initDB() {
        assertDoesNotThrow(() -> myDBManager.initDB(), "Initializing should not throw an error.");
    }
}