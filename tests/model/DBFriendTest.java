package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableModel;
import java.beans.PropertyChangeSupport;

import static org.junit.jupiter.api.Assertions.*;

class DBFriendTest {

    private DBFriend myDBFriend;
    private PropertyChangeSupport myPCS;

    @BeforeEach
    void setUp() {
        myPCS = new PropertyChangeSupport(this);
        myDBFriend = new DBFriend(myPCS);
    }

    @Test
    void getTableModel() {
        String[] queryFilters = {"testFile", "txt", "/path/to/file", "CREATE", "2024-01-01", "2024-01-02"};
        assertDoesNotThrow(() -> myDBFriend.getTableModel(queryFilters), "getTableModel() should not throw exceptions.");

        myPCS.addPropertyChangeListener(ModelProperties.TABLE_MODEL_QUERY, evt -> {
            assertNotNull(evt.getNewValue());
            assertTrue(evt.getNewValue() instanceof DefaultTableModel);
        });

        myDBFriend.getTableModel(queryFilters);
    }

    @Test
    void sendEmail() {
        String testEmail = "test@example.com";

        assertDoesNotThrow(() -> myDBFriend.sendEmail(testEmail));
    }
}