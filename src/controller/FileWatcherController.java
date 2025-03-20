package controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;

import model.DBFriend;
import model.ModelProperties;
import model.SystemWatch;
import view.ViewProperties;

/**
 * This class manages file watching, updates the database, and handles UI interactions.
 */
public class FileWatcherController implements PropertyChangeListener {

    private final SystemWatch mySysWatch;
    private final PropertyChangeSupport myPCS;
    private final DBFriend myDBFriend;

    /**
     * Initializes the file watcher controller and sets up event listeners.
     *
     * @param thePCS Notifies other components about changes.
     * @param theSystemWatch Monitors file changes in specified directories.
     */
    public FileWatcherController(final PropertyChangeSupport thePCS, final SystemWatch theSystemWatch) {
        myPCS = thePCS;
        mySysWatch = theSystemWatch;
        myDBFriend = new DBFriend(myPCS);
        myPCS.addPropertyChangeListener(this);
//        mySysWatch.addDir(Path.of(System.getProperty("user.home")));
    }

    /**
     * Handles actions like starting/stopping file watching, updating directories, managing logs, and working with the database.
     *
     * @param theEvent The event containing information about the change.
     */
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
                // mySysWatch.removeDir(receivedInputRemove[0], Path.of(receivedInputRemove[1]), false);
                mySysWatch.removeDir(receivedInputRemove[0], Path.of(receivedInputRemove[1]), Boolean.parseBoolean(receivedInputRemove[2]));
                break;
            case ViewProperties.CLEAR_LOG:
                System.out.println("clearing table");
                mySysWatch.clearLog();
                break;
            case ViewProperties.SAVE_LOG:
                mySysWatch.saveToDB();
                break;
            case ViewProperties.DB_QUERY:
                 myDBFriend.getTableModel((String[]) theEvent.getNewValue());
                break;
            case ViewProperties.EMAIL:
                myDBFriend.sendEmail((String) theEvent.getNewValue());
                break;
            default:
                break;
        }
    }
}
