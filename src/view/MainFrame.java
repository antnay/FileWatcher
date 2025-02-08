package view;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import model.ModelProperties;

public class MainFrame extends JFrame implements PropertyChangeListener {

    private Map<String, JMenuItem> myMenuMap;
    private final PropertyChangeSupport myPCS;
    private ControlPanel myControlPanel;
    // private LogPanel myLogPanel;

    public MainFrame() {
        setTitle("FileWatcher");
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myMenuMap = new HashMap<>();
        myPCS = new PropertyChangeSupport(this);
        initMenuBar();
        initFrames();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileItem = new JMenu("File");
        menuBar.add(fileItem);
        fileItem.add(createMenuItem("Start", theE -> {
            myPCS.firePropertyChange(ViewProperties.START_STOP_BUTTON, null, null);
        }, Optional.of("startStop"), Optional.of(KeyEvent.VK_ENTER))); // change state once clicked to display stop
        // fileItem.add(createMenuItem("stop", null, KeyEvent.VK_S));
        fileItem.add(createMenuItem("Save", theE -> {
            myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
        }, Optional.empty(), Optional.of(KeyEvent.VK_S)));
        JMenu helpItem = new JMenu("Help");
        menuBar.add(helpItem);
        helpItem.add(createMenuItem("About", theE -> {
            myPCS.firePropertyChange(ViewProperties.ABOUT, null, null);
        }, Optional.empty(), Optional.empty()));
        helpItem.add(createMenuItem("Show Shortcuts", theE -> {
            myPCS.firePropertyChange(ViewProperties.SHORTCUTS, null, null);
        }, Optional.of("shortcuts"), Optional.of(KeyEvent.VK_K)));

        setJMenuBar(menuBar);
    }

    private void initFrames() {
        myControlPanel = new ControlPanel(myPCS);
        add(myControlPanel);
        // myLogPanel = new LogPanel(myPCS);
    }

    /**
     * Helper method for making menu items.
     * 
     * @param theName
     * @param theAction
     * @param theHotKey
     * @return
     */
    private JMenuItem createMenuItem(final String theText, ActionListener theAction,
            final Optional<String> theName, final Optional<Integer> theHotKey) {
        JMenuItem menuItem = new JMenuItem(theText);
        theName.ifPresent(name -> menuItem.setName(name));
        menuItem.addActionListener(theAction);
        theHotKey.ifPresent(hotKey -> menuItem.setAccelerator(
                KeyStroke.getKeyStroke(hotKey, KeyEvent.META_DOWN_MASK)));
        myMenuMap.put((theName.isPresent()) ? theName.get() : theText, menuItem);
        return menuItem;
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
                break;
            case ModelProperties.STOP:
                myControlPanel.updateStartStopButt(false);
                break;
            default:
                break;
        }
    }
}
