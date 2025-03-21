package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    private Event myEvent;
    private final String testExtension = "txt";
    private final String testFileName = "testFile.txt";
    private final String testPath = "/test/path";
    private final String testEventKind = "CREATE";
    private LocalDateTime testTimeStamp;

    @BeforeEach
    void setUp() {
        myEvent = new Event(testExtension, testFileName, testPath, testEventKind);
        testTimeStamp = myEvent.getTimeStamp();
    }

    @Test
    void getExtension() {
        assertEquals(testExtension, myEvent.getExtension(), "Extension should match.");
    }

    @Test
    void getFileName() {
        assertEquals(testFileName, myEvent.getFileName(), "File name should match.");
    }

    @Test
    void getPath() {
        assertEquals(testPath, myEvent.getPath(), "Path should match.");
    }

    @Test
    void geEventKind() {
        assertEquals(testEventKind, myEvent.geEventKind(), "Event type should match.");
    }

    @Test
    void getTimeStamp() {
        assertNotNull(myEvent.getTimeStamp(), "Timestamp should not be null.");
        assertEquals(testTimeStamp, myEvent.getTimeStamp(), "Timestamp should stay the same.");
    }

    @Test
    void toArray() {
        Object[] expectedArray = {testExtension, testFileName, testPath, testEventKind, testTimeStamp};
        assertArrayEquals(expectedArray, myEvent.toArray(), "Array should match event details.");
    }

    @Test
    void testToString() {
        String expectedString = "file: " + testFileName +
                "\nextension: " + testExtension +
                "\npath: " + testPath +
                "\nkind: " + testEventKind +
                "\ntime: " + testTimeStamp;

        assertEquals(expectedString, myEvent.toString(), "String format should match.");
    }
}