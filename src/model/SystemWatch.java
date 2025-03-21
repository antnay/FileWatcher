package model;

import javax.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * A file system monitoring service that watches directories for changes.
 * Tracks file creation, modification, and deletion events and persists them to a database.
 */
public class SystemWatch {

    /**
     * Directory where log and database files are stored.
     */
    private static Path LOG_DIR = Path.of(new File("database").getAbsolutePath());

    /**
     * PropertyChangeSupport for notifying listeners of events.
     */
    private final PropertyChangeSupport myPCS;

    /**
     * Map of directory paths to their associated PathObjects.
     */
    private final ConcurrentHashMap<Path, PathObject> myPathMap;

    /**
     * Map of directory paths to their associated WatchObjects.
     */
    private Map<Path, WatchObject> myWatched;

    /**
     * WatchService for monitoring file system events.
     */
    private WatchService myWatchService;

    /**
     * Executor service for running background tasks.
     */
    private ExecutorService myExecutor;

    /**
     * Flag indicating whether the watch service is running.
     */
    private boolean myIsRunning;

    /**
     * Constructs a SystemWatch with the specified property change support.
     * Attempts to connect to the database.
     *
     * @param propertyChangeSupport The property change support used for event notifications
     */
    public SystemWatch(PropertyChangeSupport propertyChangeSupport) {
        myWatchService = null;
        try {
            DBManager.getDBManager().connect();
        } catch (DatabaseException theE) {
            System.err.println("Cannot connect to database: " + theE.getMessage());
            theE.printStackTrace();
        }
        myIsRunning = false;
        myPCS = propertyChangeSupport;
        myPathMap = new ConcurrentHashMap<>();
    }

    /**
     * Starts the file system monitoring service.
     *
     * @throws IllegalStateException if the watch service is already running
     */
    public void startWatch() {
        if (myIsRunning) {
            throw new IllegalStateException("System watch is already running");
        }
        try {
            myWatchService = FileSystems.getDefault().newWatchService();
            DBManager.getDBManager().clearTempTable();
            DBManager.getDBManager().clearWatchTable();
        } catch (IOException theE) {
            System.err.println("IO error when starting watch service: " + theE.getMessage());
            theE.printStackTrace();
        } catch (DatabaseException theE) {
            System.err.println("Database error when starting watch service: " + theE.getMessage());
            theE.printStackTrace();
            throw new IllegalStateException(theE.getCause());
        }
        myWatched = new ConcurrentHashMap<>();
        myIsRunning = true;
        myExecutor = Executors.newSingleThreadExecutor();
        runLogger();
    }

    /**
     * Stops the file system monitoring service.
     * Cancels all active watch keys and shuts down the executor.
     *
     * @throws IllegalStateException if the watch service is not running
     */
    public void stopWatch() {
        if (!myIsRunning) {
            throw new IllegalStateException("System watch is not running");
        }
        myIsRunning = false;
        if (myWatched != null) {
            for (WatchObject wo : myWatched.values()) {
                try {
                    if (wo.getWatchKeyActive()) {
                        wo.cancelWatchKey();
                    }
                } catch (Exception theE) {
                    System.err.println("Error canceling watch key: " + theE.getMessage());
                }
            }
            myWatched.clear();
        }
        try {
            if (myWatchService != null) {
                myWatchService.close();
            }
        } catch (IOException theE) {
            System.err.println("Error closing watch service: " + theE.getMessage());
        }
        myWatchService = null;
        myWatched = null;
        if (myExecutor != null) {
            myExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        myExecutor = null;
        System.out.println("shut down executor");
    }

    /**
     * Clears the event log in the database.
     *
     * @throws IllegalStateException if not connected to a database
     */
    public void clearLog() {
        if (!DBManager.getDBManager().isConnected()) {
            throw new IllegalStateException("Not connected a database");
        }
        try {
            DBManager.getDBManager().clearTempTable();
        } catch (DatabaseException theE) {
            System.err.println("Error clearing log: " + theE.getMessage());
        }
        myPCS.firePropertyChange(ModelProperties.CLEAR_TABLE, null, null);
    }

    public void clearDatabase() {
        if (!DBManager.getDBManager().isConnected()) {
            throw new IllegalStateException("Not connected a database");
        }
        try {
            DBManager.getDBManager().clearTable();
        } catch (DatabaseException theE) {
            // TODO Auto-generated catch block
        }
        myPCS.firePropertyChange(ModelProperties.CLEAR_TABLE, null, null);
    }

    /**
     * Checks if the watch service is currently running.
     *
     * @return true if the watch service is running, false otherwise
     */
    public boolean isRunning() {
        return myIsRunning;
    }

    /**
     * Adds a directory to the watch list with the specified file extension filter.
     *
     * @param theExtension      File extension to monitor (e.g., ".txt")
     * @param theDirectory      Directory path to monitor
     * @param theRecursivelyAdd Whether to recursively monitor subdirectories
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void addDir(final String theExtension, final Path theDirectory, final boolean theRecursivelyAdd) {
        if (theDirectory == null) {
            throw new IllegalArgumentException("Directory is null");
        } else if (theExtension == null) {
            throw new IllegalArgumentException("Extension is null");
        } else if (Files.notExists(theDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Directory does not exist");
        } else if (theExtension.isEmpty()) {
            throw new IllegalArgumentException("Cannot add extension: " + theExtension);
        }

        PathObject pO = myPathMap.get(theDirectory);
        if (pO == null) {
            myPathMap.put(theDirectory, new PathObject(new HashSet<>(), new AtomicInteger(1), theRecursivelyAdd));
        } else {
            pO.incrementAtomicInt();
        }
        if (!theExtension.equals(".*")) {
            myPathMap.get(theDirectory).addExt(theExtension);
        }
        if (isRunning()) {
            if (theRecursivelyAdd) {
                registerDirTree(theDirectory, false, null);
            } else {
                try {
                    registerDirectory(theDirectory);
                } catch (IllegalAccessException theE) {
                    System.err.println("Could not add directory: " + theDirectory);
                }
            }
        }
    }

    /**
     * Removes a directory from the watch list.
     *
     * @param theExtension         File extension to stop monitoring
     * @param theDirectory         Directory path to stop monitoring
     * @param theRecursivelyRemove Whether to recursively remove monitoring from subdirectories
     * @throws IllegalArgumentException if directory or extension is not being watched
     */
    public void removeDir(final String theExtension, final Path theDirectory, final boolean theRecursivelyRemove) {
        HashSet<String> extSet = myPathMap.get(theDirectory).getExts();
        if (!myPathMap.containsKey(theDirectory)) {
            throw new IllegalArgumentException("Directory is not in path table");
        } else if (!extSet.isEmpty() && !extSet.contains(theExtension)) {
            throw new IllegalArgumentException("Extension is not in watch list");
        }
        if (myPathMap.get(theDirectory).decrementAtomicInt() == 0) {
            myPathMap.remove(theDirectory);
        } else {
            myPathMap.get(theDirectory).getExts().remove(theExtension);
        }
        if (isRunning()) {
            if (theRecursivelyRemove) {
                unregisterDirectoryRecursive(theDirectory);
            } else {
                unregisterDirectory(theDirectory);
            }
        }
    }

    /**
     * Saves temporary events to the persistent database.
     *
     * @throws IllegalStateException if not connected to database
     */
    public void saveToDB() {
        if (!DBManager.getDBManager().isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        try {
            DBManager.getDBManager().mergeTempEvents();
            DBManager.getDBManager().clearTempTable();
        } catch (DatabaseException theE) {
            System.err.println("Error saving to database: " + theE.getMessage());
        }
    }

    /**
     * Starts the logging threads that monitor file system events.
     * One thread registers paths and another processes events.
     */
    private void runLogger() {
        myExecutor.submit(() -> {
            new Thread(this::registerPathMap).start();
        });
        myExecutor.submit(() -> {
            WatchKey key;
            try {
                while ((key = myWatchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        String fileName = event.context().toString();
                        Path path = ((Path) key.watchable()).resolve(fileName);
                        WatchEvent.Kind<?> eType = event.kind();
                        Path matchKey = myPathMap.keySet().stream().filter(path::startsWith).findFirst().orElse(null);
                        if (path.toFile().isDirectory() || myWatched.containsKey(path)) {
                            if (eType == StandardWatchEventKinds.ENTRY_CREATE) {
                                new Thread(() -> {
                                    if (myPathMap.get(matchKey).isRecursive()) {
                                        registerDirTree(path, true, matchKey);
                                    } else {
                                        try {
                                            registerDirectory(path);
                                        } catch (IllegalAccessException theE) {
                                            System.err.println("Could not add path: " + path);
                                        }
                                    }
                                }).start();
                            } else if (eType == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.err.println("removing directory from watch");
                                new Thread(() -> {
                                    if (myPathMap.get(matchKey).isRecursive()) {
                                        unregisterDirectoryRecursive(path);
                                    } else {
                                        unregisterDirectory(path);
                                    }
                                }).start();
                                ResultSet rS = DBManager.getDBManager().getWatchFiles(path);
                                if (rS != null) {
                                    try {
                                        while (rS.next()) {
                                            regEvent(event.kind().toString(), rS.getString("filename"), path);
                                        }
                                    } catch (SQLException theE) {
                                        System.err.println("Could not create delete event: " + theE.getMessage());
                                    }
                                }
                            }
                            continue;
                        }
                        if (myPathMap.get(matchKey).isRecursive()) {
                            regEventRecursive(event.kind().toString(), fileName, path, matchKey);
                        } else {
                            regEvent(event.kind().toString(), fileName, path);
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException | ClosedWatchServiceException | NullPointerException theEvent) {
                System.err.println("Watch service interrupted or closed: " + theEvent.getMessage());
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Registers all directories in the path map for watching.
     * Creates a thread pool to register directories in parallel.
     */
    private void registerPathMap() {
        Instant now = Instant.now();
        try (ExecutorService regExecutor = Executors.newCachedThreadPool()) {
            myPathMap.forEach((theDirectory, myOptions) -> {
                try {
                    registerDirectory(theDirectory);
                } catch (Exception theE) {
                    System.err.println("Could not add directory: " + theDirectory);
                    throw new IllegalArgumentException("Could not add directory", theE);
                }
                if (myOptions.isRecursive()) {
                    try (Stream<Path> stream = Files.list(theDirectory)) {
                        stream.filter(Files::isDirectory)
                                .filter(this::checkIfSystem)
                                .forEach(path -> regExecutor.submit(() -> {
                                    registerDirTree(path, false, null);
                                }));
                    } catch (IOException e) {
                        System.err.println("Error listing directories: " + e.getMessage());
                    }
                    regExecutor.shutdown();
                }
            });
        }
        System.out.println("Time (s): " + Duration.between(now, Instant.now()).getSeconds());
    }

    /**
     * Recursively registers a directory tree for watching.
     * Adds all files to the watch database and registers all subdirectories.
     *
     * @param theRoot        The root directory to start from
     * @param theIsNewEvent  Whether this is for a newly created directory
     * @param theWatchedPath The parent watched path
     */
    private void registerDirTree(Path theRoot, boolean theIsNewEvent, Path theWatchedPath) {
        myPCS.firePropertyChange(ModelProperties.REGISTER_START, null, null); // if gui needs to be held until done
        try {
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path theCurrentPath, BasicFileAttributes theAttrs) throws IOException {
                    if (Files.isRegularFile(theCurrentPath)) {
                        try {
                            DBManager.getDBManager().addToWatch(theCurrentPath.toFile());
                        } catch (DatabaseException e) {
                            System.err.println("Error adding file to watch database: " + e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (theIsNewEvent) {
                        Path root = myPathMap.keySet().stream().filter(dir::startsWith).findFirst().orElse(null);
                        try (Stream<Path> stream = Files.list(dir)) {
                            stream.filter(Files::isRegularFile)
                                    .forEach(file -> regEventRecursive(
                                            StandardWatchEventKinds.ENTRY_CREATE.toString(),
                                            file.getFileName().toString(), file.toAbsolutePath(), root));

                        } catch (IOException e) {
                            System.err.println("Error listing directories: " + e.getMessage());
                        }
                    }

                    if (Files.isSymbolicLink(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        handleRegisterDirectory(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    if (exc instanceof FileSystemLoopException || exc instanceof AccessDeniedException) {
                        System.err.println("Access denied: " + file);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    System.err.println("Failed to visit file: " + file + " - " + exc.getMessage());
                    exc.printStackTrace();
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error walking directory: " + e.getMessage());
        }

        if (myWatched.containsKey(LOG_DIR)) {
            myWatched.get(LOG_DIR).decrementAtomicInt();
            myWatched.remove(LOG_DIR);
        }
        myPCS.firePropertyChange(ModelProperties.REGISTER_DONE, null, null);
    }

    /**
     * Registers a single directory for watching.
     *
     * @param thePath The directory path to register
     * @throws IllegalAccessException If access to the directory is denied
     */
    private void registerDirectory(final Path thePath) throws IllegalAccessException {
        handleRegisterDirectory(thePath);
        if (myWatched.containsKey(LOG_DIR)) {
            myWatched.get(LOG_DIR).decrementAtomicInt();
            myWatched.remove(LOG_DIR);
        }
    }

    /**
     * Handles the logic for registering a directory with the watch service.
     * Either increments the reference count or creates a new watch.
     *
     * @param thePath The directory path to register
     */
    private void handleRegisterDirectory(final Path thePath) {
        if (myWatched.get(thePath) != null) {
            WatchObject wO = myWatched.get(thePath);
            wO.incrementAtomicInt();
        } else {
            WatchKey wK = null;
            try {
                wK = thePath.register(myWatchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException theE) {
                System.err.println("IO Exception: " + theE.getMessage());
                System.err.println("IO Exception path: " + thePath);
            }
            myWatched.put(thePath, new WatchObject(wK, new AtomicInteger(1)));
        }
    }

    /**
     * Records a file system event for non-recursive watches.
     *
     * @param theEvent    The event type
     * @param theFileName The file name
     * @param thePath     The file path
     */
    private void regEvent(String theEvent, String theFileName, Path thePath) {
        Path directoryPath = thePath.getParent();
        if (myPathMap.get(directoryPath).getExts().contains(getExtension(theFileName))
                || myPathMap.get(directoryPath).getExts().isEmpty()) {
            Event logEvent = getEvent(theEvent, theFileName, thePath);
            try {
                DBManager.getDBManager().addEvent(logEvent);
                myPCS.firePropertyChange(ModelProperties.EVENT, null, logEvent);
            } catch (DatabaseException theE) {
                System.err.println("Error adding event to database: " + theE.getMessage());
            }
        }
    }

    /**
     * Records a file system event for recursively watched directories.
     *
     * @param theEvent    The event type
     * @param theFileName The file name
     * @param thePath     The file path
     * @param theRoot     The root watched directory
     */
    private void regEventRecursive(String theEvent, String theFileName, Path thePath, Path theRoot) {
        PathObject pO = myPathMap.get(theRoot);
        if (pO != null && (pO.getExts().contains(getExtension(theFileName))
                || pO.getExts().isEmpty())) {
            Event logEvent = getEvent(theEvent, theFileName, thePath);
            try {
                DBManager.getDBManager().addEvent(logEvent);
                myPCS.firePropertyChange(ModelProperties.EVENT, null, logEvent);
            } catch (DatabaseException theE) {
                System.err.println("Error adding recursive event to database: " + theE.getMessage());
            }
        }
    }

    /**
     * Creates an Event object for a file system event.
     *
     * @param theEvent    The event type
     * @param theFileName The file name
     * @param thePath     The file path
     * @return A new Event object
     */
    private Event getEvent(String theEvent, String theFileName, Path thePath) {
        String extension = getExtension(theFileName);
        return new Event(extension, theFileName, thePath.getParent().toString(), theEvent);
    }

    /**
     * Extracts the file extension from a filename.
     *
     * @param theFileName The file name
     * @return The file extension including the dot, or empty string if none
     */
    private String getExtension(final String theFileName) {
        String extension = "";
        int i = theFileName.length() - 1;
        while (i >= 0) {
            char currentChar = theFileName.charAt(i);
            if (currentChar == '.') {
                if (i == 0) {
                    break;
                }
                extension = theFileName.substring(i);
                break;
            }
            i--;
        }
        return extension;
    }

    /**
     * Recursively unregisters a directory tree from watching.
     *
     * @param theRoot The root directory to unregister
     */
    private void unregisterDirectoryRecursive(Path theRoot) {
        try {
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                @Nonnull
                public FileVisitResult preVisitDirectory(Path theCurrentDir, BasicFileAttributes attrs) {
                    try {
                        if (Files.isSymbolicLink(theCurrentDir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else if (Files.isDirectory(theCurrentDir)) {
                            unregisterDirectory(theCurrentDir);
                            return FileVisitResult.CONTINUE;
                        }
                    } catch (ClosedWatchServiceException theE) {
                        System.err.println("Watch service closed: " + theE.getMessage());
                        return FileVisitResult.TERMINATE;
                    } catch (SecurityException theE) {
                        System.err.println("Security exception: " + theE.getMessage());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (IOException theE) {
            System.err.println("Could not unregister directory: " + theE.getMessage());
        }
    }

    /**
     * Unregisters a single directory from watching.
     *
     * @param theDirectory The directory to unregister
     */
    private void unregisterDirectory(Path theDirectory) {
        if (Files.isDirectory(theDirectory)) {
            System.err.println(theDirectory);
            handleUnregister(theDirectory);
        }
    }

    /**
     * Handles the logic for unregistering a directory.
     * Decrements the reference counter and removes if zero.
     *
     * @param theDirectory The directory to unregister
     */
    private void handleUnregister(Path theDirectory) {
        if (myWatched.containsKey(theDirectory)) {
            WatchObject wO = myWatched.get(theDirectory);
            if (wO.decrementAtomicInt() == 0) {
                myWatched.remove(theDirectory);
            }
        }
    }

    /**
     * Checks if a path is a system directory that should be excluded.
     * Handles different system paths based on operating system.
     *
     * @param thePath The path to check
     * @return true if the path is not a system directory, false otherwise
     */
    private boolean checkIfSystem(Path thePath) {
        String system = System.getProperty("os.name");
        if (system.contains("Mac OS")) {
            return !(thePath.toString().contains("/System"));
        }
        if (system.contains("Windows")) {
            return !(thePath.toString().matches(".:\\\\Windows"));
        }
        if (system.contains("Linux")) {
            return !(thePath.toString().contains("/proc"));
        }
        return true;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If {@code listener} is null, no exception is thrown and no action
     * is taken.
     *
     * @param theListener The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(final PropertyChangeListener theListener) {
        myPCS.addPropertyChangeListener(theListener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     * If {@code listener} was added more than once to the same event
     * source, it will be notified one less time after being removed.
     * If {@code listener} is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param theListener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(final PropertyChangeListener theListener) {
        myPCS.removePropertyChangeListener(theListener);
    }

    /**
     * Helper class that stores path-related data including extensions and reference counting.
     */
    private static class PathObject {
        /**
         * Reference counter for this path.
         */
        private final AtomicInteger myAtomicInteger;

        /**
         * Set of file extensions to watch.
         */
        private HashSet<String> myExts;

        /**
         * Flag indicating whether to watch subdirectories recursively.
         */
        private boolean myRecursive;

        /**
         * Constructs a PathObject.
         *
         * @param theSet           Set of file extensions to watch
         * @param theAtomicInteger Reference counter
         * @param theIsRecursive   Whether to watch subdirectories
         */
        private PathObject(HashSet<String> theSet, AtomicInteger theAtomicInteger, boolean theIsRecursive) {
            myExts = theSet;
            myAtomicInteger = theAtomicInteger;
            myRecursive = true;
        }

        /**
         * Gets the set of watched file extensions.
         *
         * @return Set of file extensions
         */
        private HashSet<String> getExts() {
            return myExts;
        }

        /**
         * Gets the reference counter.
         *
         * @return The atomic integer reference counter
         */
        private AtomicInteger getAtomicInteger() {
            return myAtomicInteger;
        }

        /**
         * Adds a file extension to watch.
         *
         * @param theExt File extension to add
         */
        private void addExt(String theExt) {
            myExts.add(theExt);
        }

        /**
         * Removes a file extension from watch.
         *
         * @param theExt File extension to remove
         */
        private void removeExt(String theExt) {
            myExts.remove(theExt);
        }

        /**
         * Increments the reference counter.
         *
         * @return The new counter value
         */
        private int incrementAtomicInt() {
            return myAtomicInteger.incrementAndGet();
        }

        /**
         * Decrements the reference counter.
         *
         * @return The new counter value
         */
        private int decrementAtomicInt() {
            return myAtomicInteger.decrementAndGet();
        }

        /**
         * Checks if this path is watched recursively.
         *
         * @return true if watched recursively, false otherwise
         */
        private boolean isRecursive() {
            return myRecursive;
        }
    }

    /**
     * Helper class that stores watch-related data including WatchKey and reference counting.
     */
    private static class WatchObject {
        /**
         * Reference counter for this watch.
         */
        private final AtomicInteger myAtomicInteger;

        /**
         * WatchKey for the directory.
         */
        private WatchKey myWatchKey;

        /**
         * Flag indicating whether the WatchKey is active.
         */
        private boolean myWatchKeyActive;

        /**
         * Constructs a WatchObject.
         *
         * @param theWK            WatchKey for the directory
         * @param theAtomicInteger Reference counter
         */
        private WatchObject(WatchKey theWK, AtomicInteger theAtomicInteger) {
            myWatchKey = theWK;
            myAtomicInteger = theAtomicInteger;
            myWatchKeyActive = true;
        }

        /**
         * Gets the WatchKey.
         *
         * @return The WatchKey
         */
        private WatchKey getWatchKey() {
            return myWatchKey;
        }

        /**
         * Gets the reference counter.
         *
         * @return The atomic integer reference counter
         */
        private AtomicInteger getAtomicInteger() {
            return myAtomicInteger;
        }

        /**
         * Checks if the WatchKey is active.
         *
         * @return true if active, false otherwise
         */
        private boolean getWatchKeyActive() {
            return myWatchKeyActive;
        }

        /**
         * Cancels the WatchKey.
         *
         * @throws IllegalStateException if the WatchKey is not active
         */
        private void cancelWatchKey() {
            if (!myWatchKeyActive) {
                throw new IllegalStateException("Cannot cancel inactive watchkey");
            }
            myWatchKeyActive = false;
            myWatchKey.cancel();
            myWatchKey = null;
        }

        /**
         * Increments the reference counter.
         *
         * @return The new counter value
         */
        private int incrementAtomicInt() {
            return myAtomicInteger.incrementAndGet();
        }

        /**
         * Decrements the reference counter and cancels WatchKey if zero.
         *
         * @return The new counter value
         */
        private int decrementAtomicInt() {
            int val = myAtomicInteger.decrementAndGet();
            if (val == 0) {
                cancelWatchKey();
            }
            return val;
        }
    }
}
