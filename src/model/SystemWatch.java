package model;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedList;

class SystemWatch {

    Logger myLogger;
    List<Path> myDirs;
    List<String> myExts;
    
    public SystemWatch() {
        myLogger = new Logger();
        myDirs = new LinkedList<>();
        myExts = new LinkedList<>();
    }

    public void startWatch() {

    }

    public void stopWatch() {

    }

    public void addDir(Path theDirectory) {
        if (theDirectory == null) {
            throw new IllegalArgumentException("Directory is null");
        } else if (Files.notExists(theDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Directory does not exist");
        }
        myDirs.add(theDirectory);
    }

    public void removeDir(Path theDirectory) {
        if (!myDirs.contains(theDirectory)) {
            throw new IllegalArgumentException("Directory is not in watch list");
        }
        myDirs.remove(theDirectory);
    }

    public void addExt(String theExtension) {

    }

    public void removeExt(String theExtension) {
        if (!myExts.contains(theExtension)) {
            throw new IllegalArgumentException("Directory is not in watch list");
        }
        myExts.remove(theExtension);
    }
}