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
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SystemWatch {

    private final List<String> myExts;
    private final Queue<Event> myEventQueue;
    private final List<Path> myPathList;
    private final PropertyChangeSupport myPCS;
    private Map<Path, WatchKey> myWatchKeys;
    private WatchService myWatchService;
    private ExecutorService myExecutor;
    private boolean myIsRunning;
    private int count = 0;

    public SystemWatch() {
        myWatchService = null;
        DBManager.getDBManager().connect();
        myExts = new LinkedList<>();
        myEventQueue = new ConcurrentLinkedQueue<>();
        myPathList = new LinkedList<>();
        myIsRunning = false;
        myPCS = new PropertyChangeSupport(this);
    }

    public void startWatch() {
        if (myIsRunning) {
            throw new IllegalStateException("System watch is already running");
        }
        try {
            myWatchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
//        myWatchKeys = new ConcurrentHashMap<>();
//        myWatchKeys.forEach((path, watchKey) -> {
//            WatchKey res = registerDirectory(path);
//            if (res == null) {
//                myWatchKeys.remove(path);
//                return;
//            }
//            watchKey = res;
//        });
        myIsRunning = true;
        myExecutor = Executors.newSingleThreadExecutor();
        runLogger();
        myPCS.firePropertyChange(ModelProperties.START, null, null);
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
        myPCS.firePropertyChange(ModelProperties.STOP, null, null);
        System.out.println("shut down executor");
    }

    public void clearLog() {
        if (!DBManager.getDBManager().isConnected()) {
            throw new IllegalStateException("Not connected a database");
        }
        DBManager.getDBManager().clearTable();
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
        if (!myWatchKeys.containsKey(theDirectory)) {
            throw new IllegalArgumentException("Directory is not in watch list");
        }
        // FIXME: get null pointer here if watchservice not running
        // TODO: need to walk dir and cancel :(
        myWatchKeys.get(theDirectory).cancel();
        myWatchKeys.remove(theDirectory);
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

    public void saveToLog() {
        if (!DBManager.getDBManager().isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        int size = myEventQueue.size();
        if (!myEventQueue.isEmpty()) {
            DBManager dBInstance = DBManager.getDBManager();
            Event curEvent = myEventQueue.poll();
            System.out.println(curEvent.getFileName());
            for (int i = 0; i < size; i++) {
                dBInstance.addEvent(curEvent);
                curEvent = myEventQueue.poll();
            }
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
        Event logEvent = new Event(extension, theFileName, thePath.toString(),
                theEvent, LocalDateTime.now());
        myEventQueue.add(logEvent);
        myPCS.firePropertyChange(ModelProperties.EVENT, null, logEvent);
    }

    private void runLogger() {
        myExecutor.submit(() -> {
            new Thread(this::registerPathList).start();
            WatchKey key;
            try {
//                while ((key = myWatchService.poll(1, TimeUnit.SECONDS)) != null) {
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
//        myPathList.forEach(thePath -> walk(thePath, false));
        myPathList.forEach(theRoot -> registerDirTree(theRoot, false));
        System.out.println("Time (s): " + Duration.between(now, Instant.now()).getSeconds());
    }

    private void walk(Path thePath, boolean theAddFiles) {
        File root = new File(thePath.toString());
        File[] list = root.listFiles();

        if (list == null) return;

        for (File file : list) {
            if (theAddFiles && file.isFile()) {
//                System.out.println("File: " + file.getAbsolutePath());
                regEvent(StandardWatchEventKinds.ENTRY_CREATE.toString(), file.getName(), thePath);
            } else if (file.isDirectory()) {
                System.out.println(thePath);
                registerDirectory(thePath);
                walk(Path.of(file.toURI()), theAddFiles);
            }
        }
    }

    private void registerDirTree(Path theRoot, boolean theEventSpec) {
        int[] intarray = new int[]{1, 2, 3};
        try {
            System.out.println("trying to walk: " + theRoot.toFile());
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                // FIXME: Nullpointer when shutting down executor while walking
                public FileVisitResult preVisitDirectory(Path theCurrentDir, BasicFileAttributes attrs) {
                    try {
                        if (Files.isRegularFile(theCurrentDir)) {
                            regEvent(StandardWatchEventKinds.ENTRY_CREATE.toString(), theCurrentDir.getFileName().toString(), theCurrentDir);
                        }
                        if (Files.isSymbolicLink(theCurrentDir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else if (Files.isDirectory(theCurrentDir)) {
                            if (registerDirectory(theCurrentDir) == null) {
                                throw new IllegalStateException("System not watching");
                            }
                            count++;
                            return FileVisitResult.CONTINUE;
                        }
                    } catch (SecurityException theE) {
                        // TODO Auto-generated catch block
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
        } catch (IOException e) {
            System.out.println("Could not register ");
            // TODO Auto-generated catch block
        }
        System.out.println("Done walking");
        myPCS.firePropertyChange(ModelProperties.REGISTER_ALL, null, null); // if gui needs to be held until done registering
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

//    private Event createEvent() {
//    }

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
