package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeSupport;

import static org.junit.jupiter.api.Assertions.*;

class DBFriendTest {

    private DBFriend myDBFriend;
    private PropertyChangeSupport myPCS;

    @BeforeEach
    void setUp() {
        myDBFriend = new DBFriend(myPCS);
        myPCS = new PropertyChangeSupport(this);
    }

    @Test
    void getTableModel() {
        String[] queryFilters = {"testFile", "txt", "/path/to/file", "CREATE", "2024-01-01", "2024-01-02"};
        assertDoesNotThrow(() -> myDBFriend.getTableModel(queryFilters), "getTableModel() should not throw exceptions.");
    }

    @Test
    void sendEmail() {
        String testEmail = "test@example.com";

        assertDoesNotThrow(() -> myDBFriend.sendEmail(testEmail),
                "sendEmail() should not throw exceptions.");
    }
}