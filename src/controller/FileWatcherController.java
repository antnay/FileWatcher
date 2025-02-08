package controller;

import java.nio.file.Path;

import model.DBManager;
import model.SystemWatch;

public class FileWatcherController {

    private SystemWatch sysWatch;

    public FileWatcherController() {
        sysWatch = new SystemWatch();
        sysWatch.addDir(Path.of(System.getProperty("user.home")));
        sysWatch.startWatch();
    }
}
