package controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;

import model.ModelProperties;
import model.SystemWatch;
import view.MainFrame;
import view.ViewProperties;

public class FileWatcherController implements PropertyChangeListener {

    private SystemWatch mySysWatch;
    private MainFrame myMainFrame;

    public FileWatcherController(MainFrame theView, SystemWatch theSystemWatch) {
        myMainFrame = theView;
        myMainFrame.addPropertyChangeListener(this);
        mySysWatch = theSystemWatch;
        mySysWatch.addPropertyChangeListener(this);
        mySysWatch.addDir(Path.of(System.getProperty("user.home")));
    }

    @Override
    public void propertyChange(PropertyChangeEvent theEvent) {
        switch (theEvent.getPropertyName()) {
            case ModelProperties.START:
                System.out.println("model started logging");
                break;
            case ModelProperties.STOP:
                System.out.println("model stopped logging");
                break;
            case ViewProperties.START_STOP_BUTTON:
                System.out.println("start stop button pressed");
                // TODO: if running then stop else run
                mySysWatch.startWatch();
                break;
            case ViewProperties.SUBMIT_BUTTON:
                System.out.println("submit button pressed");
                break;
            default:
                break;
        }
    }
}
