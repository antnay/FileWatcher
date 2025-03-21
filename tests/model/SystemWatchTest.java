package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;


import static org.junit.jupiter.api.Assertions.*;

class SystemWatchTest {

    private SystemWatch systemWatch;
    private PropertyChangeSupport propertyChangeSupport;

    @BeforeEach
    void setUp() {
        propertyChangeSupport = new PropertyChangeSupport(this);
        systemWatch = new SystemWatch(propertyChangeSupport);
    }

    @Test
    void startWatch() {
        assertFalse(systemWatch.isRunning());

        systemWatch.startWatch();
        assertTrue(systemWatch.isRunning());

        assertThrows(IllegalStateException.class, systemWatch::startWatch);
    }

    @Test
    void stopWatch() {
        systemWatch.startWatch();
        assertTrue(systemWatch.isRunning());

        systemWatch.stopWatch();
        assertFalse(systemWatch.isRunning());

        assertThrows(IllegalStateException.class, systemWatch::stopWatch);
    }

    @Test
    void clearLog() {
        assertDoesNotThrow(systemWatch::clearLog);
    }

    @Test
    void isRunning() {
        assertFalse(systemWatch.isRunning());
        systemWatch.startWatch();
        assertTrue(systemWatch.isRunning());
        systemWatch.stopWatch();
        assertFalse(systemWatch.isRunning());
    }

    @Test
    void addDir() {
        Path testDir = Paths.get(System.getProperty("user.home"));

        assertDoesNotThrow(() -> systemWatch.addDir(".txt", testDir, false), "It should allow adding a valid directory.");
        assertThrows(IllegalArgumentException.class, () -> systemWatch.addDir(null, testDir, false), "It should not allow adding a null extension.");
        assertThrows(IllegalArgumentException.class, () -> systemWatch.addDir(".txt", null, false), "It should not allow adding a null directory.");
        assertThrows(IllegalArgumentException.class, () -> systemWatch.addDir("", testDir, false), "It should not allow adding an empty extension.");
    }

    @Test
    void removeDir() {
        Path testDir = Paths.get(System.getProperty("user.home"));

        systemWatch.addDir(".txt", testDir, false);

        assertDoesNotThrow(() -> systemWatch.removeDir(".txt", testDir, false), "It should allow removing an added directory.");
        assertThrows(IllegalArgumentException.class, () -> systemWatch.removeDir(".txt", testDir, false), "It should not allow removing non-existent directory.");
    }

    @Test
    void saveToDB() {
        assertDoesNotThrow(systemWatch::saveToDB);
    }

    @Test
    void addPropertyChangeListener() {
        PropertyChangeListener listener = event -> {};
        systemWatch.addPropertyChangeListener(listener);

        assertDoesNotThrow(() -> systemWatch.addPropertyChangeListener(listener));
    }

    @Test
    void removePropertyChangeListener() {
        PropertyChangeListener listener = event -> {};
        systemWatch.addPropertyChangeListener(listener);
        systemWatch.removePropertyChangeListener(listener);

        assertDoesNotThrow(() -> systemWatch.removePropertyChangeListener(listener));
    }
}