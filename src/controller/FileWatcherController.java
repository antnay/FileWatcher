package controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;

import model.ModelProperties;
import model.SystemWatch;
import view.MainFrame;
import view.ViewProperties;

public class FileWatcherController implements PropertyChangeListener {

    private final SystemWatch mySysWatch;
    private final MainFrame myMainFrame;

    public FileWatcherController(MainFrame theView, SystemWatch theSystemWatch) {
        myMainFrame = theView;
        myMainFrame.addPropertyChangeListener(this);
        mySysWatch = theSystemWatch;
        mySysWatch.addPropertyChangeListener(this);
        mySysWatch.addPropertyChangeListener(theView);
        //mySysWatch.addDir(Path.of(System.getProperty("user.home")));
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
                mySysWatch.addDir((Path) theEvent.getNewValue());
                break;
            case ViewProperties.REMOVED_FROM_FILE_LIST_MODEL:
                System.out.println("stop button pressed");
                mySysWatch.removeDir((Path) theEvent.getNewValue());
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
