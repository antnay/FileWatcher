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
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SystemWatch {
    private final PropertyChangeSupport myPCS;
    private final Map<Path, HashSet<String>> myPathMap;
    // private Map<Path, WatchKey> myWatchKeys;
    private Map<Path, WatchObject> myWatched;
    private WatchService myWatchService;
    private ExecutorService myExecutor;
    private boolean myIsRunning;
    private int count = 0;

    public SystemWatch(PropertyChangeSupport propertyChangeSupport) {
        myWatchService = null;
        try {
            DBManager.getDBManager().connect();
        } catch (DatabaseException theE) {
            System.err.println("Cannot connect to database " + theE.getCause());
        }
        // myExts = new LinkedList<>();
        // myPathList = new LinkedList<>();
        myIsRunning = false;
        myPCS = propertyChangeSupport;
        myPathMap = new HashMap<>();
    }

    public void startWatch() {
        if (myIsRunning) {
            throw new IllegalStateException("System watch is already running");
        }
        try {
            myWatchService = FileSystems.getDefault().newWatchService();
            DBManager.getDBManager().clearTempTable();
            DBManager.getDBManager().clearWatchTable();
        } catch (IOException theE) {
            // TODO Auto-generated catch block
        } catch (DatabaseException theE) {
            throw new IllegalStateException(theE.getCause());
        }
        // myWatchKeys = new ConcurrentHashMap<>();
        myWatched = new ConcurrentHashMap<>();
        // myActivePaths = ConcurrentHashMap.newKeySet();
        myIsRunning = true;
        myExecutor = Executors.newSingleThreadExecutor();
        runLogger();
    }

    public void stopWatch() {
        if (!myIsRunning) {
            throw new IllegalStateException("System watch is not running");
        }
        myIsRunning = false;
        try {
            myWatchService.close();
            DBManager.getDBManager().clearWatchTable();
        } catch (IOException theE) {
            System.err.println("Error closing systemWatch: " + theE.getMessage());
            // FIXME: do something??
        } catch (DatabaseException e) {
            // TODO:
            throw new RuntimeException(e);
        }
        myWatchService = null;
        myWatched = null;
        myExecutor.shutdownNow();
        count = 0;
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
        myPathMap.computeIfAbsent(theDirectory, k -> new HashSet<>());
        myPathMap.get(theDirectory).add(theExtension);
        myPathMap.get(theDirectory).add(Boolean.toString(theRecursivelyAdd));
        if (isRunning()) {
            if (theRecursivelyAdd) {
                registerDirTree(theDirectory, false, null);
            } else {
                try {
                    registerDirectory(theDirectory);
                } catch (IllegalAccessException theE) {
                    System.err.println("Could not add: " + theDirectory);
                }
            }
        }
        // myPathMap.forEach((key, value) -> System.out.println(key + ":" + value));
        // myPathList.add(theDirectory);
        // addExt(theExtension);
    }

    public void removeDir(final String theExtension, final Path theDirectory, final boolean theRecursivelyRemove) {
        // TODO: empty extset means watch all
        HashSet<String> extSet = myPathMap.get(theDirectory);
        // if (isRunning() && !myWatchKeys.containsKey(theDirectory)) {
        // throw new IllegalArgumentException("Directory is not in watch list");
        // } else
        if (!myPathMap.containsKey(theDirectory)) {
            throw new IllegalArgumentException("Directory is not in path table");
        } else if (!extSet.isEmpty() && !extSet.contains(theExtension)) {
            throw new IllegalArgumentException("Extension is not in watch list");
        }
        // } else if (!myExts.contains(theExtension)) {
        // throw new IllegalArgumentException("Extension is not in watch list");
        // }

        myPathMap.remove(theDirectory);
        if (isRunning()) {
            if (theRecursivelyRemove) {
                unregisterDirectoryRecursive(theDirectory);
            } else {
                unregisterDirectory(theDirectory);
            }
        }
        // removeExt(theExtension);
    }

    public void saveToDB() {
        if (!DBManager.getDBManager().isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        try {
            DBManager.getDBManager().mergeTempEvents();
            DBManager.getDBManager().clearTempTable();
        } catch (DatabaseException theE) {
            // TODO Auto-generated catch block
        }
    }

    private void runLogger() {
        myExecutor.submit(() -> {
            new Thread(this::registerPathMap).start();
            WatchKey key;
            try {
                // while ((key = myWatchService.poll(1, TimeUnit.SECONDS)) != null) {
                while ((key = myWatchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        String fileName = event.context().toString();
                        Path path = ((Path) key.watchable()).resolve(fileName);
                        WatchEvent.Kind<?> eType = event.kind();
                        // FIXME: when deleting directory, path.isdirectory does not work
                        // FIXME: if you have an extension being watched in a directory, if you delete
                        // that directory then the files will be reported as modified instead of deleted
                        Path matchKey = myPathMap.keySet().stream().filter(path::startsWith).findFirst().orElse(null);
                        if (path.toFile().isDirectory()) {
                            if (eType == StandardWatchEventKinds.ENTRY_CREATE) {
                                // TODO: background thread
                                new Thread(() -> {
                                    if (myPathMap.get(matchKey).contains("true")) {
                                        registerDirTree(path, true, matchKey);
                                    } else {
                                        try {
                                            registerDirectory(path);
                                        } catch (IllegalAccessException theE) {
                                            System.err.println("Could not add: " + path);
                                        }
                                    }
                                }).start();
                            } else if (eType == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.out.println("removing directory from watch");
                                // TODO: remove directory tree
                            }
                            continue;
                        }
                        if (myPathMap.get(matchKey).contains("true")) {
                            regEventRecursive(event.kind().toString(), fileName, path, matchKey);
                        } else {
                            regEvent(event.kind().toString(), fileName, path);
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException | ClosedWatchServiceException theEvent) {
                Thread.currentThread().interrupt();
            }
//            catch (AccessDeniedException e) {
//                System.out.println("DEBUG : Runtime exception in runLogger");
//                throw new RuntimeException(e);
//            } catch (IOException e) {
//                System.out.println("DEBUG : IO exception in runLogger");
//                throw new RuntimeException(e);
//            }
        });
    }

    private void registerPathMap() {
        Instant now = Instant.now();
        // myPathList.forEach(theRoot -> registerDirTree(theRoot, false));
        myPathMap.forEach((theDirectory, myOptions) -> {
            if (myOptions.contains("true")) {
                registerDirTree(theDirectory, false, null);
            } else {
                try {
                    registerDirectory(theDirectory);
                } catch (Exception theE) {
                    System.err.println("Could not add: " + theDirectory);
                    throw new IllegalArgumentException("Could not add directory");
                }
            }
        });
        System.out.println("Time (s): " + Duration.between(now, Instant.now()).getSeconds());
    }

    private void registerDirTree(Path theRoot, boolean theIsNewEvent, Path theWatchedPath) {
        myPCS.firePropertyChange(ModelProperties.REGISTER_START, null, null); // if gui needs to be held until done
        final boolean[] fail = {false};
        try {
            System.out.println("im walking hyeah: " + theRoot.toFile());
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                @Nonnull
                public FileVisitResult preVisitDirectory(Path theCurrentDir, BasicFileAttributes theAttrs) {
                    try {
                        if (theIsNewEvent) {
                            File[] list = theCurrentDir.toFile().listFiles();
                            if (list != null) {
                                for (File file : list) {
                                    if (file.isFile()) {
                                        System.out.println(file.getName());
                                        regEventRecursive(StandardWatchEventKinds.ENTRY_CREATE.toString(),
                                                file.getName(), file.toPath(), theWatchedPath);
                                    }
                                }
                            }
                        }
                        if (Files.isSymbolicLink(theCurrentDir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else if (Files.isDirectory(theCurrentDir)) {
                            if (checkIfSystem(theCurrentDir)) {
                                System.err.println("cant register directory " + theCurrentDir);
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            handleRegisterDirectory(theCurrentDir);
                            count++;
                            return FileVisitResult.CONTINUE;
                        }
                    } catch (ClosedWatchServiceException theE) {
                        fail[0] = true;
                        return FileVisitResult.TERMINATE;
                    } catch (SecurityException theE) {
                        System.err.println("Could not add: " + theCurrentDir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }

                // TOO SLOW :(
//                @Override
//                public FileVisitResult visitFile(Path theCurrentPath, BasicFileAttributes theAttrs) throws IOException {
//                    if (Files.isRegularFile(theCurrentPath)) {
//                        DBManager.getDBManager().addToWatch(theCurrentPath.toFile());
//                    }
//                    return FileVisitResult.CONTINUE;
//                }

                @Override
                @Nonnull
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    if (exc instanceof FileSystemLoopException || exc instanceof AccessDeniedException) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException theE) {
            System.err.println(theE.getMessage());
        }

        if (fail[0]) {
            System.out.println("Done walking with errors");
        } else {
            Path logDir = Path.of(new File("database").getAbsolutePath());
            if (myWatched.containsKey(logDir)) {
                myWatched.get(logDir).cancelWatchKey();
                myWatched.get(logDir).decrementAtomicInt();
                myWatched.remove(logDir);
            }
            System.out.println("Done walking");
        }
        myPCS.firePropertyChange(ModelProperties.REGISTER_DONE, null, null); // if gui needs to be held until done
        System.out.println(count);
    }

    private void registerDirectory(final Path thePath) throws IllegalAccessException {
//        System.out.println(thePath); // DEBUG
//        new Thread(() -> {
//            File[] list = thePath.toFile().listFiles();
//            if (list != null) {
//                for (File file : list) {
//                    if (file.isFile()) {
//                        DBManager.getDBManager().addToWatch(file);
//                    }
//                }
//            }
//        }).start();
        if (checkIfSystem(thePath)) {
            System.err.println("cant register directory " + thePath);
        }
        handleRegisterDirectory(thePath);
    }

    private void handleRegisterDirectory(final Path thePath) {
        if (myWatched.containsKey(thePath)) {
            myWatched.get(thePath).incrementAtomicInt();
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
        // TODO: something about pathmap
    }

    // TODO refactor this at some point
    private void regEvent(String theEvent, String theFileName, Path thePath) {
        Path directoryPath = thePath.getParent();
        if (myPathMap.get(directoryPath).contains(getExtension(theFileName)) || myPathMap.get(directoryPath).contains(".*")) {
            Event logEvent = getEvent(theEvent, theFileName, thePath);
            try {
                DBManager.getDBManager().addEvent(logEvent);
                myPCS.firePropertyChange(ModelProperties.EVENT, null, logEvent);
                System.out.println("ModelProperties.EVENT was fired");
            } catch (DatabaseException theE) {
                // TODO Auto-generated catch block
            }
        }
    }

    /**
     * Like regEvent but used when the event is from a file already exists. This
     * case can occur when dragging and dropping a directory into a watched
     * directory.
     *
     * @param theEvent
     * @param theFileName
     * @param thePath
     * @param theRoot     is the watched directory in myPathMap
     */
    private void regEventRecursive(String theEvent, String theFileName, Path thePath, Path theRoot) {
        if (myPathMap.get(theRoot).contains(getExtension(theFileName)) || myPathMap.get(theRoot).contains(".*")) {
            Event logEvent = getEvent(theEvent, theFileName, thePath);
            try {
                DBManager.getDBManager().addEvent(logEvent);
                myPCS.firePropertyChange(ModelProperties.EVENT, null, logEvent);
                System.out.println("ModelProperties.EVENT was fired");
            } catch (DatabaseException theE) {
                System.err.println(theE.getMessage());
                // TODO Auto-generated catch block
            }
        }

    }

    private Event getEvent(String theEvent, String theFileName, Path thePath) {
        String extension = getExtension(theFileName);
        return new Event(extension, theFileName, thePath.getParent().toString(), theEvent);
    }

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
                        return FileVisitResult.TERMINATE;
                    } catch (SecurityException theE) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (IOException theE) {
            System.err.println("Could not register " + theE.getMessage());
            // TODO Auto-generated catch block
        }
    }

    private void unregisterDirectory(Path theDirectory) {
        if (Files.isDirectory(theDirectory)) {
            System.out.println(theDirectory);
            handleUnregister(theDirectory);
        }
    }

    private void handleUnregister(Path theDirectory) {
        /*
         * TODO:
         * 1. If the path exists in myPathMap, it is an actively watched root directory.
         * 2. If the path is in myWatchKeys but not in myPathMap, it is part of a
         * recursive watch but not actively selected.
         * 3. Before stopping a WatchKey, check if any parent path in myPathMap is
         * recursively watched.
         */
        if (myWatched.containsKey(theDirectory)) {
            if (myWatched.get(theDirectory).decrementAtomicInt() == 0) {
                myWatched.get(theDirectory).cancelWatchKey();
                myWatched.remove(theDirectory);
            }
            // TODO: something about pathmap
        }
    }

    private boolean checkIfSystem(Path thePath) {
        String system = System.getProperty("os.name");
//        System.err.println("Operating system: " + system);
        return switch (system) {
            case "Mac OS X" -> (thePath.toString().equals("/System"));
            case "Windows" -> (thePath.toString().equals("Windows"));
//            case "Linux" -> (thePath.toString().equals("system"));
            default -> false;
        };
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

    private static class WatchObject {

        private final WatchKey myWatchKey;
        private final AtomicInteger myAtomicInteger;
        private boolean myWatchKeyActive;

        private WatchObject(WatchKey theWK, AtomicInteger theAtomicInteger) {
            myWatchKey = theWK;
            myAtomicInteger = theAtomicInteger;
            myWatchKeyActive = true;
        }

        private WatchKey getWatchKey() {
            return myWatchKey;
        }

        private AtomicInteger getAtomicInteger() {
            return myAtomicInteger;
        }

        private boolean getWatchKeyActive() {
            return myWatchKeyActive;
        }

        private void cancelWatchKey() {
            if (!myWatchKeyActive) {
                throw new IllegalStateException("Cannot cancel inactive watchkey");
            }
            myWatchKeyActive = false;
            myWatchKey.cancel();
        }

        private int incrementAtomicInt() {
            return myAtomicInteger.incrementAndGet();
        }

        private int decrementAtomicInt() {
            return myAtomicInteger.decrementAndGet();
        }
    }

}
