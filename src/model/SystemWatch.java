package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
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
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SystemWatch {

    private final List<String> myExts;
    // private final Queue<Event> myEventQueue;
    private final List<Path> myPathList;
    private final PropertyChangeSupport myPCS;
    private Map<Path, WatchKey> myWatchKeys;
    private WatchService myWatchService;
    private ExecutorService myExecutor;
    private boolean myIsRunning;
    private int count = 0;

    public SystemWatch() {
        myWatchService = null;
        try {
            DBManager.getDBManager().connect();
        } catch (DatabaseException theE) {
            // TODO Auto-generated catch block
        }
        myExts = new LinkedList<>();
        // myEventQueue = new ConcurrentLinkedQueue<>();
        myPathList = new LinkedList<>();
        myIsRunning = false;
        myPCS = new PropertyChangeSupport(this);
    }

    public void startWatch() {
        if (myIsRunning) {
            throw new IllegalStateException("System watch is already running");
        }
        try {
            DBManager.getDBManager().clearTempTable();
        } catch (DatabaseException theE) {
            // TODO Auto-generated catch block
        }
        try {
            myWatchService = FileSystems.getDefault().newWatchService();
        } catch (IOException theE) {
            // TODO Auto-generated catch block
        }
        myWatchKeys = new ConcurrentHashMap<>();
        myIsRunning = true;
        myExecutor = Executors.newSingleThreadExecutor();
        runLogger();
        //myPCS.firePropertyChange(ModelProperties.START, null, null);
    }

    public void stopWatch() {
        if (!myIsRunning) {
            throw new IllegalStateException("System watch is not running");
        }
        myIsRunning = false;
        try {
            myWatchService.close();
        } catch (IOException theE) {
            System.err.println("Error closing systemWatch: " + theE.getMessage());
        }
        myWatchService = null;
        myWatchKeys = null;
        myExecutor.shutdownNow();
//        myPCS.firePropertyChange(ModelProperties.STOP, null, null);
        System.out.println("shut down executor");
    }

    public void clearLog() {
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

    public boolean isRunning() {
        return myIsRunning;
    }

    public void addDir(final Path theDirectory) {
        if (theDirectory == null) {
            throw new IllegalArgumentException("Directory is null");
        } else if (Files.notExists(theDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Directory does not exist");
        }
        myPathList.add(theDirectory);
    }

    public void removeDir(final Path theDirectory) {
        if (!isRunning()) {
            throw new IllegalStateException("System watch is not running");
        } else if (!myWatchKeys.containsKey(theDirectory)) {
            throw new IllegalArgumentException("Directory is not in watch list");
        }
        unregisterDirectory(theDirectory);
    }

    public void addExt(String theExtension) {
        if (theExtension.isEmpty()) {
            throw new IllegalArgumentException("Cannot add extension: " + theExtension);
        }
        myExts.add(theExtension);
    }

    public void removeExt(String theExtension) {
        if (!myExts.contains(theExtension)) {
            throw new IllegalArgumentException("Extension is not in watch list");
        }
        myExts.remove(theExtension);
    }

    public void saveToDB() {
        if (!DBManager.getDBManager().isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        try {
            // FIXME: infinite loop is back
            DBManager.getDBManager().mergeTempEvents();
            DBManager.getDBManager().clearTempTable();
        } catch (DatabaseException theE) {
            // TODO Auto-generated catch block
        }
    }

    private void regEvent(String theEvent, String theFileName, Path thePath) {
        String extension = "";
        int i = theFileName.length() - 1;
        while (i >= 0) {
            char currentChar = theFileName.charAt(i);
            if (currentChar == '.') {
                if (i == 0) {
                    break;
                }
                extension = theFileName.substring(i + 1);
                break;
            }
            i--;
        }
        Event logEvent = new Event(extension, theFileName, thePath.toString(), theEvent);
        try {
            DBManager.getDBManager().addEvent(logEvent);
            myPCS.firePropertyChange(ModelProperties.EVENT, null, logEvent);
        } catch (DatabaseException theE) {
            // TODO Auto-generated catch block
        }
        // myEventQueue.add(logEvent);
    }

    private void runLogger() {
        myExecutor.submit(() -> {
            new Thread(this::registerPathList).start();
            WatchKey key;
            try {
                // while ((key = myWatchService.poll(1, TimeUnit.SECONDS)) != null) {
                while ((key = myWatchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        String fileName = event.context().toString();
                        Path path = ((Path) key.watchable()).resolve(fileName);
                        WatchEvent.Kind<?> eType = event.kind();
                        // FIXME: when deleting directory, path.isdirectory does not work
                        if (path.toFile().isDirectory()) {
                            if (eType == StandardWatchEventKinds.ENTRY_CREATE) {
                                registerDirTree(path, true);
                            } else if (eType == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.out.println("removing directory from watch");
                                // TODO: remove directory tree
                            }
                            continue;
                        }
                        regEvent(event.kind().toString(), fileName, path);
                    }
                    key.reset();
                }
            } catch (InterruptedException | ClosedWatchServiceException theEvent) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void registerPathList() {
        Instant now = Instant.now();
        myPathList.forEach(theRoot -> registerDirTree(theRoot, false));
        System.out.println("Time (s): " + Duration.between(now, Instant.now()).getSeconds());
    }

    private void registerDirTree(Path theRoot, boolean theEventSpec) {
        myPCS.firePropertyChange(ModelProperties.REGISTER_START, null, null); // if gui needs to be held until done
        try {
            System.out.println("im walking hyeah: " + theRoot.toFile());
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                // FIXME: Nullpointer when shutting down executor while walking
                public FileVisitResult preVisitDirectory(Path theCurrentDir, BasicFileAttributes attrs) {
                    try {
                        if (Files.isRegularFile(theCurrentDir)) {
                            regEvent(StandardWatchEventKinds.ENTRY_CREATE.toString(),
                                    theCurrentDir.getFileName().toString(), theCurrentDir);
                        } else if (Files.isSymbolicLink(theCurrentDir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else if (Files.isDirectory(theCurrentDir)) {
                            WatchKey wK = registerDirectory(theCurrentDir);
                            if (wK == null) {
                                throw new IllegalStateException("System not watching");
                            } else {
                                myWatchKeys.put(theCurrentDir, wK);
                            }
                            count++;
                            return FileVisitResult.CONTINUE;
                        }
                    } catch (SecurityException | IllegalStateException theE) {
                        // TODO Auto-generated catch block
                        System.err.println("ERRRRRRRRRRRRRRRRRRRRRROR " + theE.getMessage());
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    if (exc instanceof FileSystemLoopException) {
                        System.err.println("Filesystem loop detected: " + file);
                    } else {
                        System.err.println("Error accessing file: " + file + " - " + exc.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException theE) {
            System.err.println("Could not register " + theE.getMessage());
            // TODO Auto-generated catch block
        }

        Path logDir = Path.of(new File("database").getAbsolutePath());
        if (myWatchKeys.containsKey(logDir)) {
            myWatchKeys.get(logDir).cancel();
            myWatchKeys.remove(logDir);
        }
        System.out.println("Done walking");
        myPCS.firePropertyChange(ModelProperties.REGISTER_DONE, null, null); // if gui needs to be held until done
        System.out.println(count);
    }

    private WatchKey registerDirectory(final Path thePath) {
        try {
            return thePath.register(myWatchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

        } catch (IOException | ClosedWatchServiceException theE) {
            System.err.println("Error adding path to WatchService");
            return null;
        }
    }

    private void unregisterDirectory(Path theRoot) {
        try {
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                // FIXME: Nullpointer when shutting down executor while walking
                public FileVisitResult preVisitDirectory(Path theCurrentDir, BasicFileAttributes attrs) {
                    try {
                        if (Files.isSymbolicLink(theCurrentDir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else if (Files.isDirectory(theCurrentDir)) {
                            System.out.println(theCurrentDir);
                            if (myWatchKeys.containsKey(theCurrentDir)) {
                                myWatchKeys.get(theCurrentDir).cancel();
                                myWatchKeys.remove(theCurrentDir);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    } catch (SecurityException | IllegalStateException theE) {
                        // TODO Auto-generated catch block
                        System.err.println("ERRRRRRRRRRRRRRRRRRRRRROR " + theE.getMessage());
                        ;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (IOException theE) {
            System.err.println("Could not register " + theE.getMessage());
            // TODO Auto-generated catch block
        }
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

}
