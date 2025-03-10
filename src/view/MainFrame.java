package view;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import model.ModelProperties;

public class MainFrame extends JFrame implements PropertyChangeListener {

    private final static String CONFIRM_CLOSE_RESPONSE = "Yes";
    private final static String CANCEL_CLOSE_RESPONSE = "No";
    private final static String SAVE_CLOSE_RESPONSE = "Save Changes and Close";
    private final PropertyChangeSupport myPCS;
    private JMenuItem myStartMItem;
    private JMenuItem myStopMItem;
    private JButton myStartToolbarButton;
    private JButton myStopToolbarButton;
    private int myLogTableRowCount = 0;

    public MainFrame(PropertyChangeSupport propertyChangeSupport) {
        myPCS = propertyChangeSupport;
        myPCS.addPropertyChangeListener(this);
        setTitle("FileWatcher");
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            setSize(680, 600);
        } else {
            setSize(600, 600);
        }
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // if there are rows in the log table
                if (myLogTableRowCount > 0) {
                    String[] responses = {CONFIRM_CLOSE_RESPONSE, CANCEL_CLOSE_RESPONSE, SAVE_CLOSE_RESPONSE};
                    int userResponse = JOptionPane.showOptionDialog(
                            null,
                            "Are you sure you want to exit?",
                            "Changes Not Saved to Database",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            responses,
                            responses[2]);
                    if (userResponse == -1) { // if the user closes the JOptionPane
                        userResponse = 1; // set their response to CANCEL_CLOSE_RESPONSE
                    }
                    switch (responses[userResponse]) {
                        case CONFIRM_CLOSE_RESPONSE:
                            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                            //dispatchEvent(new WindowEvent(MainFrame.this, WindowEvent.WINDOW_CLOSED));
                            break;
                        case CANCEL_CLOSE_RESPONSE:
                            break;
                        case SAVE_CLOSE_RESPONSE:
                            System.out.println("TODO: MAKE THE PROGRAM SAVE TO THE DATABASE HERE");
                            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                            break;
                        default:
                            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                            break;
                    }
                } else {
                    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                }
            }
        });
        setLayout(new BorderLayout());
        initMenuBar();
        initToolBar();
        initFrames();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileItem = new JMenu("File");
        menuBar.add(fileItem);
        JMenu helpItem = new JMenu("Help");
        menuBar.add(helpItem);

        myStartMItem = new JMenuItem("Start Logging");
        addActionToStartButtons(myStartMItem);
        myStartMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK));
        fileItem.add(myStartMItem);

        myStopMItem = new JMenuItem("Stop Logging");
        addActionToStopButtons(myStopMItem);
        myStopMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK));
        myStopMItem.setEnabled(false);
        fileItem.add(myStopMItem);

        JMenuItem saveLogMItem = new JMenuItem("Save Log");
        saveLogMItem.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
        });
        saveLogMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.META_DOWN_MASK));
        fileItem.add(saveLogMItem);

        JMenuItem openDBFrameItem = new JMenuItem("SQL Query Executor");
        openDBFrameItem.addActionListener(e -> new DBFrame(myPCS).setVisible(true));
        fileItem.add(openDBFrameItem);

        JMenuItem aboutMItem = new JMenuItem("About");
        aboutMItem.addActionListener(theE -> {
            JFrame aboutFrame = new HelpFrame();
            myPCS.firePropertyChange(ViewProperties.ABOUT, null, null);
        });
        helpItem.add(aboutMItem);

        JMenuItem shortcutMItem = new JMenuItem("Show Shortcuts");
        shortcutMItem.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.SHORTCUTS, null, null);
        });
        shortcutMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.META_DOWN_MASK));
        helpItem.add(shortcutMItem);

        setJMenuBar(menuBar);
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();

        myStartToolbarButton = new JButton(new ImageIcon("src/resources/startIcon.png"));
        myStartToolbarButton.setToolTipText("Start the file watch system.");
        addActionToStartButtons(myStartToolbarButton);

        myStopToolbarButton = new JButton(new ImageIcon("src/resources/stopIcon.png"));
        myStopToolbarButton.setToolTipText("Stop the file watch system.");
        addActionToStopButtons(myStopToolbarButton);
        myStopToolbarButton.setEnabled(false);

        JButton saveToDatabaseButton = new JButton(new ImageIcon("src/resources/saveIcon.png"));
        saveToDatabaseButton.setToolTipText("Save the current watched files log to the database.");
        saveToDatabaseButton.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
        });

        JButton openDatabaseWindow = new JButton(new ImageIcon("src/resources/databaseIcon.png"));
        openDatabaseWindow.setToolTipText("Open the database query window.");
        openDatabaseWindow.addActionListener(theE -> new DBFrame(myPCS).setVisible(true));

        toolBar.add(myStartToolbarButton);
        toolBar.add(myStopToolbarButton);
        toolBar.add(saveToDatabaseButton);
        toolBar.add(openDatabaseWindow);

        this.add(toolBar, BorderLayout.NORTH);
    }

    private void initFrames() {
        JPanel framePanel = new JPanel(new BorderLayout());
        framePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel userControls = new JPanel(new BorderLayout());
        userControls.setBorder(new EmptyBorder(0, 0, 20, 0));

        ControlPanel controlPanel = new ControlPanel(myPCS);
        FileListPanel fileListPanel = new FileListPanel(myPCS);
        LogPanel logPanel = new LogPanel(myPCS);


        userControls.add(controlPanel, BorderLayout.NORTH);
        userControls.add(fileListPanel, BorderLayout.CENTER);

        framePanel.add(userControls, BorderLayout.NORTH);
        framePanel.add(logPanel, BorderLayout.CENTER);

        add(framePanel, BorderLayout.CENTER);
    }

    private void addActionToStartButtons(final AbstractButton theButton) {
        theButton.addActionListener(theE -> {
            myPCS.firePropertyChange(ModelProperties.START, null, null);
        });
    }

    private void addActionToStopButtons(final AbstractButton theButton) {
        theButton.addActionListener(theE -> {
            myPCS.firePropertyChange(ModelProperties.STOP, null, null);
        });
    }

    private void toggleStartStopButtons(final String theProperty) {
        if (theProperty.equals(ModelProperties.START)) { // if start was clicked
            myStartMItem.setEnabled(false);
            myStartToolbarButton.setEnabled(false);
            myStopMItem.setEnabled(true);
            myStopToolbarButton.setEnabled(true);
        } else { // if stop was clicked
            myStartMItem.setEnabled(true);
            myStartToolbarButton.setEnabled(true);
            myStopMItem.setEnabled(false);
            myStopToolbarButton.setEnabled(false);
        }
    }

    private void showErrorWindow(final String theErrorType) {
        String errorTitle = switch (theErrorType) {
            case InputErrorProperties.EXTENSION -> "Invalid File Extension";
            case InputErrorProperties.DIRECTORY -> "Invalid Directory";
            case InputErrorProperties.BOTH_INPUTS -> "Invalid File Extension and Directory";
            default -> "This should not appear. If it does then we need better input validation.";
        };

        JOptionPane.showMessageDialog(this, theErrorType, errorTitle, JOptionPane.ERROR_MESSAGE);
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
        // System.out.println("view pc: " + theEvent.getPropertyName());
        switch (theEvent.getPropertyName()) {
            case ModelProperties.START:
                toggleStartStopButtons(ModelProperties.START);
                break;
            case ModelProperties.STOP:
                toggleStartStopButtons(ModelProperties.STOP);
                break;
            case InputErrorProperties.BOTH_INPUTS, InputErrorProperties.EXTENSION, InputErrorProperties.DIRECTORY:
                showErrorWindow(theEvent.getPropertyName());
                break;
            default:
                break;
        }
    }

    public PropertyChangeSupport getPCS() {
        return myPCS;
    }
}
