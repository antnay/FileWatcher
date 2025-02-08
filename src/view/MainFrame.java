package view;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import model.ModelProperties;

public class MainFrame extends JFrame implements PropertyChangeListener {

    private final PropertyChangeSupport myPCS;
    private ControlPanel myControlPanel;
    // private LogPanel myLogPanel;
    private JMenuItem myStartStopMItem;
    private JMenuItem mySaveLogMItem;
    private JMenuItem myAboutMItem;
    private JMenuItem myShortcutMItem;

    public MainFrame() {
        setTitle("FileWatcher");
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myPCS = new PropertyChangeSupport(this);
        initMenuBar();
        initFrames();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileItem = new JMenu("File");
        menuBar.add(fileItem);

        myStartStopMItem = new JMenuItem("Start Logging");
        myStartStopMItem.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.START_STOP_BUTTON, null, null);
        });
        myStartStopMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK));
        fileItem.add(myStartStopMItem);


        mySaveLogMItem = new JMenuItem("Save Log");
        mySaveLogMItem.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
        });
        mySaveLogMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.META_DOWN_MASK));
        fileItem.add(mySaveLogMItem);


        myAboutMItem = new JMenuItem("About");
        myAboutMItem.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.ABOUT, null, null);
        });
        fileItem.add(myAboutMItem);


        myShortcutMItem = new JMenuItem("Show Shortcuts");
        myShortcutMItem.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.SHORTCUTS, null, null);
        });
        myShortcutMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.META_DOWN_MASK));
        fileItem.add(myShortcutMItem);

        setJMenuBar(menuBar);
    }

    private void initFrames() {
        myControlPanel = new ControlPanel(myPCS);
        add(myControlPanel);
        // myLogPanel = new LogPanel(myPCS);
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

    @Override
    public void propertyChange(PropertyChangeEvent theEvent) {
        System.out.println("view pc: " + theEvent.getPropertyName());
        switch (theEvent.getPropertyName()) {
            case ModelProperties.START:
                myControlPanel.updateStartStopButt(true);
                myStartStopMItem.setText("Stop Logging");
                break;
            case ModelProperties.STOP:
                myControlPanel.updateStartStopButt(false);
                myStartStopMItem.setText("Start Logging");
                break;
            default:
                break;
        }
    }
}
