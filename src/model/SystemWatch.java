package model;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedList;

public class SystemWatch {

    private Map<String, WatchKey> myWatchKeys;
    private List<String> myExts;
    private WatchService myWatchService;
    private ExecutorService myExecutor;
    private boolean myIsRunning;

    public SystemWatch() {
        try {
            myWatchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
        myWatchKeys = new TreeMap<>();
        myExts = new LinkedList<>();
        myIsRunning = false;
        myExecutor = Executors.newSingleThreadExecutor();
        // runLogger();
    }

    public void startWatch() {
        myIsRunning = true;
        runLogger();
    }

    public void stopWatch() {
        myIsRunning = false;
        // myExecutor.shutdownNow();
    }

    public void addDir(final Path theDirectory) {
        if (theDirectory == null) {
            throw new IllegalArgumentException("Directory is null");
        } else if (Files.notExists(theDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Directory does not exist");
        }
        try {
            System.out.println(theDirectory.toString());
            myWatchKeys.put(theDirectory.toString(),
                    theDirectory.register(myWatchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY));
            System.out.println("Successfully registered: " + theDirectory);
        } catch (IOException theEvent) {
            System.out.println(theEvent.getMessage());
        }

    }

    public void removeDir(final Path theDirectory) {
        String dirString = theDirectory.toString();
        if (!myWatchKeys.containsKey(dirString)) {
            throw new IllegalArgumentException("Directory is not in watch list");
        }
        myWatchKeys.get(dirString).cancel();
        myWatchKeys.remove(dirString);
    }

    public void addExt(String theExtension) {
        if (theExtension.isEmpty()) {
            throw new IllegalArgumentException();
        }
        myExts.add(theExtension);
    }

    public void removeExt(String theExtension) {
        if (!myExts.contains(theExtension)) {
            throw new IllegalArgumentException("Directory is not in watch list");
        }
        myExts.remove(theExtension);
    }

    private void runLogger() {
        myExecutor.submit(() -> {
            while (myIsRunning) {
                WatchKey key;
                try {
                    while ((key = myWatchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            System.out.println(event.kind()+ " " + event.context());
                        }
                        key.reset();
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                }
            }
        });
    }
}