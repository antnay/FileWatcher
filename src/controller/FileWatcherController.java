package controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;

import model.FileWatcherProperties;
import model.SystemWatch;
import view.MainFrame;

public class FileWatcherController implements PropertyChangeListener {

    private SystemWatch mySysWatch;
    private MainFrame myMainView;

    public FileWatcherController(MainFrame theView, SystemWatch theSystemWatch) {
        myMainView = theView;
        mySysWatch = theSystemWatch;
        mySysWatch.addPropertyChangeListener(this);
        mySysWatch.addDir(Path.of(System.getProperty("user.home")));
        mySysWatch.startWatch();
    }


    @Override
    public void propertyChange(PropertyChangeEvent theEvent) {
        switch (theEvent.getPropertyName()) {
            case FileWatcherProperties.START:
                break;
            case FileWatcherProperties.STOP:
                break;
            case FileWatcherProperties.SUBMIT:
                break;
            default:
                break;
        }
    }
}
