package model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    private static Email myEmail;

    @BeforeAll
    static void setUp() {
        myEmail = new Email();
    }

    @Test
    void gCheck() {
        assertTrue(Email.gCheck(), "Is unable to find Google credentials file");
    }

    @Test
    void sendEmailWithLogFile() {
        File testFile = new File("test_log.csv");
        try {
            boolean fileCreated = testFile.createNewFile();
            assertTrue(fileCreated || testFile.exists(), "Test file wasn't able to be created.");
            myEmail.sendEmailWithLogFile("test@example.com", testFile);
        } catch (Exception e) {
            fail("Exception occurred while testing email sending: " + e.getMessage());
        } finally {
            testFile.delete();
        }
    }
}