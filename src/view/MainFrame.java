package view;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import model.ModelProperties;

/**
 * The main window that the program is displayed in. When this frame is closed, the program will stop running.
 * Contains a menu bar and toolbar that allows users to interact with the program.
 */
public class MainFrame extends JFrame implements PropertyChangeListener {

    /**
     * Constant string for confirmation that the user wants to close the window.
     */
    private final static String CONFIRM_CLOSE_RESPONSE = "Yes";

    /**
     * Constant string for confirmation that the user wants to keep the window open.
     */
    private final static String CANCEL_CLOSE_RESPONSE = "No";

    /**
     * Constant string for confirmation that the user wants to save changes then close the window.
     */
    private final static String SAVE_CLOSE_RESPONSE = "Save Changes and Close";

    /**
     * <code>PropertyChangeSupport</code> for notifying listeners of events.
     */
    private final PropertyChangeSupport myPCS;

    /**
     * JMenuItem to let users start the watch service that enables and disables when it is available.
     * Provides the same functionality as myStartToolbarButton.
     */
    private JMenuItem myStartMItem;

    /**
     * JMenuItem to let users stop the watch service that enables and disables when it is available.
     * Provides the same functionality as myStopToolbarButton.
     */
    private JMenuItem myStopMItem;

    /**
     * JMenuItem to let users save to the database that enables and disables when it is available.
     * Provides the same functionality as mySaveToolbarButton.
     */
    private JMenuItem mySaveMItem;

    /**
     * JMenuItem to let users clear the database that enables and disables when it is available.
     * Provides the same functionality as myClearToolbarButton.
     */
    private JMenuItem myClearLogMItem;

    /**
     * JButton to let users start the watch service from the toolbar that enables and disables when it is available.
     * Provides the same functionality as myStartMItem.
     */
    private JButton myStartToolbarButton;

    /**
     * JButton to let users stop the watch service from the toolbar that enables and disables when it is available.
     * Provides the same functionality as myStopMItem.
     */
    private JButton myStopToolbarButton;

    /**
     * JButton to let users save to the database from the toolbar that enables and disables when it is available.
     * Provides the same functionality as mySaveMItem.
     */
    private JButton mySaveToolbarButton;

    /**
     * JButton to let users clear the database from the toolbar that enables and disables when it is available.
     * Provides the same functionality as myClearMItem.
     */
    private JButton myClearLogToolbarButton;

    /**
     * Constructs the main GUI of the program. Displays a window that users can use to control the program.
     * If the window is closed while there are unsaved changes to the database, a confirmation window will appear
     * asking the user if they want to close without saving, cancel closing the program, or save the unsaved changes
     * and then close.
     *
     * @param thePcs The <code>PropertyChangeSupport</code> that this listener should be added to.
     */
    public MainFrame(final PropertyChangeSupport thePcs) {
        myPCS = thePcs;
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
                if (mySaveMItem.isEnabled()
                        && myClearLogMItem.isEnabled()
                        && mySaveToolbarButton.isEnabled()
                        && myClearLogToolbarButton.isEnabled()) {
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
                            break;
                        case CANCEL_CLOSE_RESPONSE:
                            break;
                        case SAVE_CLOSE_RESPONSE:
                            myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
                            //System.out.println("Changes saved to database before close");
                            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                            break;
                        default:
                            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
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
        initPanels();
    }

    /**
     * Creates a menu bar for the frame and populates it with a File and Help menu. Menus can be accessed with
     * keyboard shortcuts by using mnemonics and accelerators.
     */
    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileItem = new JMenu("File");
        fileItem.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileItem);
        JMenu helpItem = new JMenu("Help");
        helpItem.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpItem);

        myStartMItem = new JMenuItem("Start Logging");
        myStartMItem.addActionListener(theE -> {
            startFunctionality();
        });
        myStartMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK));
        fileItem.add(myStartMItem);

        myStopMItem = new JMenuItem("Stop Logging");
        myStopMItem.addActionListener(theE -> {
            stopFunctionality();
        });
        myStopMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK));
        myStopMItem.setEnabled(false);
        fileItem.add(myStopMItem);

        fileItem.addSeparator();

        mySaveMItem = new JMenuItem("Save Log");
        mySaveMItem.addActionListener(theE -> {
            saveFunctionality();
        });
        mySaveMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        mySaveMItem.setEnabled(false);
        fileItem.add(mySaveMItem);

        myClearLogMItem = new JMenuItem("Clear Log");
        myClearLogMItem.addActionListener(theE -> {
            clearLogFunctionality();
        });
        myClearLogMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        myClearLogMItem.setEnabled(false);
        fileItem.add(myClearLogMItem);

        fileItem.addSeparator();

        JMenuItem openDBFrameMItem = new JMenuItem("Query Database (Opens another window)");
        openDBFrameMItem.addActionListener(e -> new DBFrame(myPCS).setVisible(true));
        openDBFrameMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        fileItem.add(openDBFrameMItem);

        JMenuItem clearDBMItem = new JMenuItem("Clear Database");
        clearDBMItem.addActionListener(theE -> {
            clearDatabaseFunctionality();
        });
        clearDBMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK));
        fileItem.add(clearDBMItem);

        JMenuItem aboutMItem = new JMenuItem("About");
        aboutMItem.addActionListener(theE -> {
            new HelpFrame();
            myPCS.firePropertyChange(ViewProperties.ABOUT, null, null);
        });
        aboutMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        helpItem.add(aboutMItem);

        setJMenuBar(menuBar);
    }

    /**
     * Creates a toolbar for the frame and populates it with buttons for each option in the File menu.
     */
    private void initToolBar() {
        JToolBar toolBar = new JToolBar();

        myStartToolbarButton = new JButton(new ImageIcon("src/resources/startIcon.png"));
        myStartToolbarButton.setToolTipText("Start the file watch system.");
        myStartToolbarButton.addActionListener(theE -> {
            startFunctionality();
        });
        toolBar.add(myStartToolbarButton);

        myStopToolbarButton = new JButton(new ImageIcon("src/resources/stopIcon.png"));
        myStopToolbarButton.setToolTipText("Stop the file watch system.");
        myStopToolbarButton.addActionListener(theE -> {
            stopFunctionality();
        });
        myStopToolbarButton.setEnabled(false);
        toolBar.add(myStopToolbarButton);

        mySaveToolbarButton = new JButton(new ImageIcon("src/resources/saveIcon.png"));
        mySaveToolbarButton.setToolTipText("Save the current watched files log to the database.");
        mySaveToolbarButton.addActionListener(theE -> {
            saveFunctionality();
        });
        mySaveToolbarButton.setEnabled(false);
        toolBar.add(mySaveToolbarButton);

        myClearLogToolbarButton = new JButton(new ImageIcon("src/resources/clearIcon.png"));
        myClearLogToolbarButton.setToolTipText("Clear the current watched files log without saving to the database.");
        myClearLogToolbarButton.addActionListener(theE -> {
            clearLogFunctionality();
        });
        myClearLogToolbarButton.setEnabled(false);
        toolBar.add(myClearLogToolbarButton);

        JButton openDatabaseWindowButton = new JButton(new ImageIcon("src/resources/databaseIcon.png"));
        openDatabaseWindowButton.setToolTipText("Open the database query window.");
        openDatabaseWindowButton.addActionListener(theE -> new DBFrame(myPCS).setVisible(true));
        toolBar.add(openDatabaseWindowButton);

        JButton clearDatabaseButton = new JButton(new ImageIcon("src/resources/clearDatabaseIcon.png"));
        clearDatabaseButton.setToolTipText("Clear all rows in the database.");
        clearDatabaseButton.addActionListener(theE -> {
            clearDatabaseFunctionality();
        });
        toolBar.add(clearDatabaseButton);

        this.add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Creates the panels needed for the window and adds them to the frame.
     */
    private void initPanels() {
        JPanel framePanel = new JPanel(new BorderLayout());
        framePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel userControls = new JPanel(new BorderLayout());
        userControls.setBorder(new EmptyBorder(0, 0, 20, 0));

        InputPanel inputPanel = new InputPanel(myPCS);
        FileListPanel fileListPanel = new FileListPanel(myPCS);
        LogPanel logPanel = new LogPanel(myPCS);


        userControls.add(inputPanel, BorderLayout.NORTH);
        userControls.add(fileListPanel, BorderLayout.CENTER);

        framePanel.add(userControls, BorderLayout.NORTH);
        framePanel.add(logPanel, BorderLayout.CENTER);

        add(framePanel, BorderLayout.CENTER);
    }

    /**
     * Helper method to add functionality to a component that will be used to start the file watcher.
     * Should be updated when a new component is added with this function.
     */
    private void startFunctionality() {
        myPCS.firePropertyChange(ModelProperties.START, null, null);

        // toggle the start button off and the stop button on
        myStartMItem.setEnabled(false);
        myStartToolbarButton.setEnabled(false);
        myStopMItem.setEnabled(true);
        myStopToolbarButton.setEnabled(true);
    }

    /**
     * Helper method to add functionality to a component that will be used to stop the file watcher.
     * Should be updated when a new component is added with this function.
     */
    private void stopFunctionality() {
        myPCS.firePropertyChange(ModelProperties.STOP, null, null);

        // toggle the stop button off and the start button on
        myStartMItem.setEnabled(true);
        myStartToolbarButton.setEnabled(true);
        myStopMItem.setEnabled(false);
        myStopToolbarButton.setEnabled(false);
    }

    /**
     * Helper method to add functionality to a component that will be used
     * to save changes caught by the file watcher to the database.
     */
    private void saveFunctionality() {
        myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
        disableSaveClearButtons();
    }

    /**
     * Helper method to add functionality to a component that will be used
     * to clear the log of changes caught by the file watcher without saving to the database.
     */
    private void clearLogFunctionality() {
        myPCS.firePropertyChange(ViewProperties.CLEAR_LOG, null, null);
        disableSaveClearButtons();
    }

    /**
     * Helper method to disable the components that allow users
     * to save and clear changes caught by the file watcher.
     * Should be updated whenever a new component is added with one of these functions.
     */
    private void disableSaveClearButtons() {
        mySaveMItem.setEnabled(false);
        mySaveToolbarButton.setEnabled(false);
        myClearLogMItem.setEnabled(false);
        myClearLogToolbarButton.setEnabled(false);
    }

    /**
     * Helper method to add functionality to a component that will be used
     * to clear all rows saved in the database. Displays a dialog that an attempt
     * was made to save the changes to the database.
     */
    private void clearDatabaseFunctionality() {
        myPCS.firePropertyChange(ViewProperties.CLEAR_DATABASE, null, null);
        JOptionPane.showMessageDialog(this, "Database cleared");
    }

    /**
     * Displays an error window when input is invalid in some way. The properties of the window will change depending on
     * <code>theErrorType</code>.
     *
     * @param theErrorType Any string from InputErrorProperties that represents which error occurred.
     */
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
     * This method gets called when a property bound in a
     * <code>PropertyChangeSupport</code> this listener is added to is changed.
     *
     * @param theEvent A PropertyChangeEvent object describing the event source
     *          and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent theEvent) {
        switch (theEvent.getPropertyName()) {
            case InputErrorProperties.BOTH_INPUTS, InputErrorProperties.EXTENSION, InputErrorProperties.DIRECTORY:
                showErrorWindow(theEvent.getPropertyName());
                break;
            case ModelProperties.LOG_LIST_MODEL_UPDATED:
                mySaveMItem.setEnabled(true);
                mySaveToolbarButton.setEnabled(true);
                myClearLogMItem.setEnabled(true);
                myClearLogToolbarButton.setEnabled(true);
            default:
                break;
        }
    }
}
