package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedList;

public class SystemWatch {

    private Map<Path, WatchKey> myWatchKeys;
    private List<String> myExts;
    private WatchService myWatchService;
    private ExecutorService myExecutor;
    private Queue<Event> myEventQueue;
    private List<Path> myPathList;
    private boolean myIsRunning;
    private final PropertyChangeSupport myPCS;

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
        myWatchKeys = new ConcurrentHashMap<>();
        try {
            myWatchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
        myWatchKeys.forEach((path, watchKey) -> {
            WatchKey res = registerDirectory(path);
            if (res == null) {
                myWatchKeys.remove(path);
                return;
            }
            watchKey = res;
        });
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
        myEventQueue.forEach(System.out::println);
        if (!myEventQueue.isEmpty()) {
            DBManager dBInstance = DBManager.getDBManager();
            Event curEvent = myEventQueue.poll();
            System.out.println(curEvent.getFileName());
            while (curEvent != null) {
                dBInstance.addEvent(curEvent);
                curEvent = myEventQueue.poll();
            }
        }
    }

    private void runLogger() {
        System.out.println("running logger");
        myExecutor.submit(() -> {
            new Thread(() -> {
                checkPaths();
            }).start();
            WatchKey key;
            try {
                while ((key = myWatchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        String fileName = event.context().toString();
                        Path path = ((Path) key.watchable()).resolve(fileName);
                        String eType = event.kind().toString();
                        // FIXME: when deleting directory, path.isdirectory does not work, find another
                        // way to check
                        if (path.toFile().isDirectory()) {
                            if (eType.contains("CREATE")) {
                                registerDirTree(path);
                                System.out.println("registering new directory");
                            }
                            if (eType.contains("DELETE")) {
                                System.out.println("removing directory from watch");
                                // TODO: remove directory tree
                            }
                            continue;
                        }
                        System.out.println("gonna get extension now");
                        String extension = "";
                        int i = fileName.length() - 1;
                        while (i >= 0) {
                            char currentChar = fileName.charAt(i);
                            if (currentChar == '.') {
                                if (i == 0) {
                                    break;
                                }
                                extension = fileName.substring(i + 1);
                            }
                            i--;
                        }
                        System.out.println("got extension");
                        Event logEvent = new Event(extension, fileName, path.toString(),
                                event.kind().toString(), LocalDateTime.now());
                        myPCS.firePropertyChange(ModelProperties.EVENT, null, logEvent);
                        System.out.println(logEvent);
                        myEventQueue.add(logEvent);
                        // System.out.println("added event to queue");
                    }
                    key.reset();
                }
            } catch (InterruptedException | ClosedWatchServiceException theEvent) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void checkPaths() {
        myPathList.forEach(rootPath -> {
            registerDirTree(rootPath);
        });
    }

    private void registerDirTree(Path theRoot) {
        try {
            System.out.println("trying to walk: " + theRoot.toFile());
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path theCurrentDir, BasicFileAttributes attrs) {
                    try {
                        if (Files.isSymbolicLink(theCurrentDir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else if (Files.isDirectory(theCurrentDir)) {
                            registerDirectory(theCurrentDir);
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
    }

    private WatchKey registerDirectory(final Path thePath) {
        try {
            return thePath.register(myWatchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException theE) {
            System.err.println("Error adding path to WatchService");
            return null;
        } catch (ClosedWatchServiceException theE) {
            System.err.println("Error adding path to WatchService");
            return null;
        }
    }

    private Event createEvent() {

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