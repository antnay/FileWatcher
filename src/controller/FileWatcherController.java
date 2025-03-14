package controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;

import model.ModelProperties;
import model.SystemWatch;
import view.ViewProperties;

public class FileWatcherController implements PropertyChangeListener {

    private final SystemWatch mySysWatch;
    private final PropertyChangeSupport myPCS;

    public FileWatcherController(final PropertyChangeSupport thePCS, final SystemWatch theSystemWatch) {
        myPCS = thePCS;
        mySysWatch = theSystemWatch;
        myPCS.addPropertyChangeListener(this);
//        mySysWatch.addDir(Path.of(System.getProperty("user.home")));
    }

    @Override
    public void propertyChange(PropertyChangeEvent theEvent) {
//        System.out.println("controller pc: " + theEvent.getPropertyName());
        switch (theEvent.getPropertyName()) {
            case ModelProperties.START:
                System.out.println("model started logging");
                mySysWatch.startWatch();
                break;
            case ModelProperties.STOP:
                System.out.println("model stopped logging");
                mySysWatch.stopWatch();
                break;
            case ViewProperties.ADDED_TO_FILE_LIST_MODEL:
                String[] receivedInputAdd = (String[]) theEvent.getNewValue();
                mySysWatch.addDir(receivedInputAdd[0], Path.of(receivedInputAdd[1]), Boolean.parseBoolean(receivedInputAdd[2]));
                // mySysWatch.addDir(receivedInputAdd[0], Path.of(receivedInputAdd[1]), false);
                break;
            case ViewProperties.REMOVED_FROM_FILE_LIST_MODEL:
                System.out.println("stop button pressed");
                String[] receivedInputRemove = (String[]) theEvent.getNewValue();
                mySysWatch.removeDir(receivedInputRemove[0], Path.of(receivedInputRemove[1]), false);
                break;
            case ViewProperties.CLEAR_LOG_BUTTON:
                System.out.println("clearing table");
                mySysWatch.clearLog();
                break;
            case ViewProperties.SAVE_LOG:
                mySysWatch.saveToDB();
            default:
                break;
        }
    }
}
