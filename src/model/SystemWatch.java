package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
// import java.nio.file.attribute.;;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.LinkedList;

public class SystemWatch {

    private Map<Path, WatchKey> myWatchKeys;
    private List<String> myExts;
    private WatchService myWatchService;
    private ExecutorService myExecutor;
    private Queue<Event> myEventQueue;
    private boolean myIsRunning;
    private final PropertyChangeSupport myPCS;

    public SystemWatch() {
        myWatchService = null;
        DBManager.getDBManager().connect();
        myWatchKeys = new TreeMap<>();
        myExts = new LinkedList<>();
        myEventQueue = new ConcurrentLinkedQueue<>();
        myIsRunning = false;
        myPCS = new PropertyChangeSupport(this);
    }

    public void startWatch() {
        if (myIsRunning) {
            throw new IllegalStateException("System watch is already running");
        }
        try {
            myWatchService = FileSystems.getDefault().newWatchService();
            myWatchKeys.forEach((path, watchKey) -> {
                try {
                    watchKey = path.register(myWatchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                } catch (IOException e) {
                    System.err.println("Error adding path to WatchService");
                }
            });
        } catch (IOException theE) {
            System.err.println("Error initializing systemWatch: " + theE.getMessage());
        }
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
        // theDirectory.register(myWatchService, StandardWatchEventKinds.ENTRY_CREATE,
        // StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY));
        myWatchService = null;
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
        myWatchKeys.put(theDirectory, null);
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
            registerDirTree();
            while (myIsRunning) {
                WatchKey key;
                Pattern pattern = Pattern.compile("\\.\\w+");
                try {
                    while ((key = myWatchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            // TODO: Check if event is directory creation, add to map and register with watchservice
                            // if directory then registerDir()
                            String fileName = event.context().toString();
                            boolean match = pattern.matcher(fileName).matches();
                            if (match) { // TODO: make me optional?
                                continue;
                            }
                            String path = ((Path) key.watchable()).resolve(fileName).toString();
                            // TODO: Get extension
                            Event logEvent = new Event("", fileName, path,
                                    event.kind().toString(), LocalDateTime.now());
                            System.out.println(logEvent);
                            myEventQueue.add(logEvent);
                            System.out.println("added event to queue");
                        }
                        key.reset();
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                }
            }
        });
    }

    private void registerDirTree() {
        myWatchKeys.keySet().forEach(rootPath -> {
            try {
                Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS).forEach(currentPath -> {
                    if (!Files.isDirectory(currentPath, LinkOption.NOFOLLOW_LINKS)) {
                        return;
                    } else if (myWatchKeys.containsKey(currentPath)) {
                        return;
                    }
                    try {
                        myWatchKeys.put(currentPath,
                                Path.of(currentPath.toString()).register(myWatchService,
                                        StandardWatchEventKinds.ENTRY_CREATE,
                                        StandardWatchEventKinds.ENTRY_DELETE,
                                        StandardWatchEventKinds.ENTRY_MODIFY));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                    }
                });
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
        });
    }

    private void registerDirectory() {
        
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