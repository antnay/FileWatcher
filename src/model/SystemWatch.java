package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SystemWatch {
    // private final List<String> myExts;
    // private final List<Path> myPathList;
    private final PropertyChangeSupport myPCS;
    private final Map<Path, HashSet<String>> myPathMap;
    private Map<Path, WatchKey> myWatchKeys;
    private WatchService myWatchService;
    private ExecutorService myExecutor;
    private boolean myIsRunning;
    private int count = 0;

    public SystemWatch(PropertyChangeSupport propertyChangeSupport) {
        myWatchService = null;
        try {
            DBManager.getDBManager().connect();
        } catch (DatabaseException theE) {
            // TODO Auto-generated catch block
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
            // FIXME: do something??
        }
        myWatchService = null;
        myWatchKeys = null;
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
        if (isRunning()) {
            try {
                System.out.println("DEBUG : not adding recursively in addDir");
                registerDirectory(theDirectory);
            } catch (IOException | SecurityException theE) {
                System.err.println("Could not add: " + theDirectory);
                throw new IllegalArgumentException("Could not add directory");
            }
        }
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

    // public void addExt(String theExtension) {
    // if (theExtension.isEmpty()) {
    // throw new IllegalArgumentException("Cannot add extension: " + theExtension);
    // }
    // myExts.add(theExtension);
    // }

    // public void removeExt(String theExtension) {
    // if (!myExts.contains(theExtension)) {
    // throw new IllegalArgumentException("Extension is not in watch list");
    // }
    // myExts.remove(theExtension);
    // }

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

    // TODO refactor this at some point
    private void regEvent(String theEvent, String theFileName, Path thePath) {
        Path directoryPath = thePath.getParent();
        if (myPathMap.get(directoryPath).contains(getExtension(theFileName))) {
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

    private static Event getEvent(String theEvent, String theFileName, Path thePath) {
        String extension = getExtension(theFileName);
        Event logEvent = new Event(extension, theFileName, thePath.getParent().toString(), theEvent);
        return logEvent;
    }

    private static String getExtension(final String theFileName) {
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
                        if (path.toFile().isDirectory()) {
                            if (eType == StandardWatchEventKinds.ENTRY_CREATE) {
                                registerDirectory(path);
                            } else if (eType == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.out.println("removing directory from watch");
                                // TODO: remove directory tree
                            }
                            continue;
                        }
                        // TODO: specific extension from pathmap
                        regEvent(event.kind().toString(), fileName, path);
                    }
                    key.reset();
                }
            } catch (InterruptedException | ClosedWatchServiceException theEvent) {
                Thread.currentThread().interrupt();
            } catch (AccessDeniedException e) {
                System.out.println("DEBUG : Runtime exception in runLogger");
                throw new RuntimeException(e);
            } catch (IOException e) {
                System.out.println("DEBUG : IO exception in runLogger");
                throw new RuntimeException(e);
            }
        });
    }

    private void registerPathMap() {
        Instant now = Instant.now();
        // myPathList.forEach(theRoot -> registerDirTree(theRoot, false));
        myPathMap.forEach((theRoot, myExtensions) -> {
            try {
                registerDirectory(theRoot);
            } catch (IOException e) {
                System.out.println("DEBUG : Runtime exception in registerPathMap");
                throw new RuntimeException(e);
            }
        });
        System.out.println("Time (s): " + Duration.between(now, Instant.now()).getSeconds());
    }

//    private void registerDirTree(Path theRoot, boolean theIsNewEvent) {
//        myPCS.firePropertyChange(ModelProperties.REGISTER_START, null, null); // if gui needs to be held until done
//        final boolean[] fail = { false };
//        try {
//            System.out.println("im walking hyeah: " + theRoot.toFile());
//            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
//                public FileVisitResult preVisitDirectory(Path theCurrentDir, BasicFileAttributes theAttrs) {
//                    try {
//                        if (Files.isRegularFile(theCurrentDir)) {
//                            regEvent(StandardWatchEventKinds.ENTRY_CREATE.toString(),
//                                    theCurrentDir.getFileName().toString(), theCurrentDir);
//                        } else if (Files.isSymbolicLink(theCurrentDir)) {
//                            return FileVisitResult.SKIP_SUBTREE;
//                        } else if (Files.isDirectory(theCurrentDir)) {
//                            System.out.println("DEBUG : adding recursively in registerDirTree");
//                            WatchKey wK = registerDirectory(theCurrentDir);
//                            myWatchKeys.put(theCurrentDir, wK);
//                            count++;
//                            return FileVisitResult.CONTINUE;
//                        }
//                    } catch (ClosedWatchServiceException theE) {
//                        fail[0] = true;
//                        return FileVisitResult.TERMINATE;
//                    } catch (IOException | SecurityException theE) {
//                        System.err.println("Could not add: " + theCurrentDir);
//                        return FileVisitResult.SKIP_SUBTREE;
//                    }
//                    return FileVisitResult.SKIP_SUBTREE;
//                }
//
//                @Override
//                public FileVisitResult visitFileFailed(Path file, IOException exc) {
//                    if (exc instanceof FileSystemLoopException || exc instanceof AccessDeniedException) {
//                        return FileVisitResult.SKIP_SUBTREE;
//                    }
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//        } catch (IOException theE) {
//            System.err.println(theE.getMessage());
//        }
//
//        if (fail[0]) {
//            System.out.println("Done walking with errors");
//        } else {
//            Path logDir = Path.of(new File("database").getAbsolutePath());
//            if (myWatchKeys.containsKey(logDir)) {
//                myWatchKeys.get(logDir).cancel();
//                myWatchKeys.remove(logDir);
//            }
//            System.out.println("Done walking");
//        }
//        myPCS.firePropertyChange(ModelProperties.REGISTER_DONE, null, null); // if gui needs to be held until done
//        System.out.println(count);
//    }

    private WatchKey registerDirectory(final Path thePath)
            throws IOException, ClosedWatchServiceException, AccessDeniedException {
        // System.out.println(thePath);
        return thePath.register(myWatchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
    }

    private void unregisterDirectory(Path theDirectory) {
        if (Files.isDirectory(theDirectory)) {
            System.out.println(theDirectory);
            if (myWatchKeys.containsKey(theDirectory)) {
                myWatchKeys.get(theDirectory).cancel();
                myWatchKeys.remove(theDirectory);
                // TODO: something about pathmap
            }
        }
    }

    private void unregisterDirectoryRecursive(Path theRoot) {
        try {
            Files.walkFileTree(theRoot, new SimpleFileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path theCurrentDir, BasicFileAttributes attrs) {
                    try {
                        if (Files.isSymbolicLink(theCurrentDir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else if (Files.isDirectory(theCurrentDir)) {
                            // System.out.println(theCurrentDir);
                            if (myWatchKeys.containsKey(theCurrentDir)) {
                                myWatchKeys.get(theCurrentDir).cancel();
                                myWatchKeys.remove(theCurrentDir);
                            }
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

    // Probably dont need this

    // private class PathObject {
    //     private final HashSet<String> myExtensions;
    //     private WatchKey myWatchKey;

    //     private PathObject(String theExtension) {
    //         myExtensions = new HashSet<>();
    //         myExtensions.add(theExtension);
    //         myWatchKey = null;
    //     }

    //     private PathObject(final HashSet<String> theExtensions) {
    //         myExtensions = theExtensions;
    //         myWatchKey = null;
    //     }

    //     private void addWatchKey(WatchKey theWK) {
    //         myWatchKey = theWK;
    //     }

    //     private void removeWatchKey() {
    //         myWatchKey = null;
    //     }

    //     private void addExtension(String theExtension) {
    //         myExtensions.add(theExtension);
    //     }

    //     private void removeExtension(String theExtension) {
    //         myExtensions.remove(theExtension);
    //     }

    //     private WatchKey getWatchKey() {
    //         return myWatchKey;
    //     }

    //     private HashSet<String> getExtensions() {
    //         return myExtensions;
    //     }

    // }
}
