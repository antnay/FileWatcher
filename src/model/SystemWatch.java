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
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class SystemWatch {

    private static Path LOG_DIR = Path.of(new File("database").getAbsolutePath());

    private final PropertyChangeSupport myPCS;
    private final ConcurrentHashMap<Path, PathObject> myPathMap;
    private Map<Path, WatchObject> myWatched;
    private WatchService myWatchService;
    private ExecutorService myExecutor;
    private boolean myIsRunning;

    public SystemWatch(PropertyChangeSupport propertyChangeSupport) {
        myWatchService = null;
        try {
            DBManager.getDBManager().connect();
        } catch (DatabaseException theE) {
            System.err.println("Cannot connect to database " + theE.getCause());
        }
        myIsRunning = false;
        myPCS = propertyChangeSupport;
        myPathMap = new ConcurrentHashMap<>();
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
        myWatched = new ConcurrentHashMap<>();
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
        } catch (IOException theE) {
            System.err.println("Error closing systemWatch: " + theE.getMessage());
        }
        myWatchService = null;
        myWatched = null;
        myExecutor.shutdownNow();
        System.out.println("shut down executor");
    }

    public void clearLog() {
        if (!DBManager.getDBManager().isConnected()) {
            throw new IllegalStateException("Not connected a database");
        }
        try {
            DBManager.getDBManager().clearTempTable();
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
                    System.err.println("Could not add: " + theDirectory);
                }
            }
        }
    }

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
        });
        myExecutor.submit(() -> {
            WatchKey key;
            try {
                while ((key = myWatchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        String fileName = event.context().toString();
                        Path path = ((Path) key.watchable()).resolve(fileName);
                        WatchEvent.Kind<?> eType = event.kind();
                        // FIXME: when deleting directory, path.isdirectory does not work
                        // FIXME: if you have an extension being watched in a directory, if you delete
                        // that directory then the files will be reported as modified instead of deleted
                        // FIXME: deleting watched directory should unregister
                        Path matchKey = myPathMap.keySet().stream().filter(path::startsWith).findFirst().orElse(null);
                        if (path.toFile().isDirectory()) {
                            if (eType == StandardWatchEventKinds.ENTRY_CREATE) {
                                new Thread(() -> {
                                    if (myPathMap.get(matchKey).isRecursive()) {
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
                                System.err.println("removing directory from watch");
                                new Thread(() -> {
                                    if (myPathMap.get(matchKey).isRecursive()) {
                                        unregisterDirectoryRecursive(matchKey);
                                    } else {
                                        unregisterDirectory(matchKey);
                                    }
                                }).start();
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
            } catch (InterruptedException | ClosedWatchServiceException theEvent) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void registerPathMap() {
        Instant now = Instant.now();
        // myPathList.forEach(theRoot -> registerDirTree(theRoot, false));
        try (ExecutorService regExecutor = Executors.newCachedThreadPool()) {
            myPathMap.forEach((theDirectory, myOptions) -> {
                try {
                    registerDirectory(theDirectory);
                } catch (Exception theE) {
                    System.err.println("Could not add: " + theDirectory);
                    throw new IllegalArgumentException("Could not add directory");
                }
                if (myOptions.isRecursive()) {
                    try (Stream<Path> stream = Files.list(theDirectory)) {
                        stream.filter(Files::isDirectory)
                                .filter(this::checkIfSystem)
                                .forEach(path -> regExecutor.submit(() -> {
                                    System.out.println(path);
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
        System.out.println();

    }

    private void registerDirTree(Path theRoot, boolean theIsNewEvent, Path theWatchedPath) {
        myPCS.firePropertyChange(ModelProperties.REGISTER_START, null, null); // if gui needs to be held until done
        try {
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                // // TOO SLOW :(
                // // @Override
                // // public FileVisitResult visitFile(Path theCurrentPath, BasicFileAttributes
                // // theAttrs) throws IOException {
                // // if (Files.isRegularFile(theCurrentPath)) {
                // // DBManager.getDBManager().addToWatch(theCurrentPath.toFile());
                // // }
                // // return FileVisitResult.CONTINUE;
                // // }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // System.out.println(dir);
                    if (theIsNewEvent) {
                        try (Stream<Path> stream = Files.list(dir)) {
                            stream.filter(Files::isRegularFile)
                                    .forEach(file ->
                                            regEventRecursive(StandardWatchEventKinds.ENTRY_CREATE.toString(),
                                                    file.getFileName().toString(), file.toAbsolutePath(), dir));

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
        // }
        myPCS.firePropertyChange(ModelProperties.REGISTER_DONE, null, null); // if gui needs to be
        // held until done

    }

    private void registerDirectory(final Path thePath) throws IllegalAccessException {
        handleRegisterDirectory(thePath);
        if (myWatched.containsKey(LOG_DIR)) {
            myWatched.get(LOG_DIR).decrementAtomicInt();
            myWatched.remove(LOG_DIR);
        }
    }

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

    // TODO refactor this at some point
    private void regEvent(String theEvent, String theFileName, Path thePath) {
        Path directoryPath = thePath.getParent();
        if (myPathMap.get(directoryPath).getExts().contains(getExtension(theFileName))
                || myPathMap.get(directoryPath).getExts().isEmpty()) {
            Event logEvent = getEvent(theEvent, theFileName, thePath);
            try {
                DBManager.getDBManager().addEvent(logEvent);
                myPCS.firePropertyChange(ModelProperties.EVENT, null, logEvent);
            } catch (DatabaseException theE) {
                // TODO Auto-generated catch block
            }
        }
    }

    /**
     * Like regEvent but used when watch is recursive.
     *
     * @param theEvent
     * @param theFileName
     * @param thePath
     * @param theRoot     is the watched directory in myPathMap
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
            System.err.println(theDirectory);
            handleUnregister(theDirectory);
        }
    }

    private void handleUnregister(Path theDirectory) {
        if (myWatched.containsKey(theDirectory)) {
            WatchObject wO = myWatched.get(theDirectory);
            if (wO.decrementAtomicInt() == 0) {
                System.out.println(wO + " " + wO.getAtomicInteger().get());
                myWatched.remove(theDirectory);
            }
            // TODO: something about pathmap
        }
    }

    private boolean checkIfSystem(Path thePath) {
        String system = System.getProperty("os.name");
        // System.err.println("Operating system: " + system);
        if (thePath.toString().contains("/System")) {
            System.out.println("OOOOOOOW WE SKIPPONG");
        }
        return switch (system) {
            case "Mac OS X" -> !(thePath.toString().contains("/System"));
            case "Windows" -> !(thePath.toString().matches(".:\\\\Windows\\."));
            case "Linux" -> !(thePath.toString().contains("/proc"));
            default -> true;
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

    private static class PathObject {
        private final AtomicInteger myAtomicInteger;
        private HashSet<String> myExts;
        private boolean myRecursive;

        private PathObject(HashSet<String> theSet, AtomicInteger theAtomicInteger, boolean theIsRecursive) {
            myExts = theSet;
            myAtomicInteger = theAtomicInteger;
            myRecursive = true;
        }

        private HashSet<String> getExts() {
            return myExts;
        }

        private AtomicInteger getAtomicInteger() {
            return myAtomicInteger;
        }

        private void addExt(String theExt) {
            myExts.add(theExt);
        }

        private void removeExt(String theExt) {
            myExts.remove(theExt);
        }

        private int incrementAtomicInt() {
            return myAtomicInteger.incrementAndGet();
        }

        private int decrementAtomicInt() {
            return myAtomicInteger.decrementAndGet();
        }

        private boolean isRecursive() {
            return myRecursive;
        }
    }

    private static class WatchObject {
        private final AtomicInteger myAtomicInteger;
        private WatchKey myWatchKey;
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
            myWatchKey = null;
        }

        private int incrementAtomicInt() {
            return myAtomicInteger.incrementAndGet();
        }

        private int decrementAtomicInt() {
            int val = myAtomicInteger.decrementAndGet();
            if (val == 0) {
                cancelWatchKey();
            }
            return val;
        }
    }

}
