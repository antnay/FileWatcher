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

    private final static String CONFIRM_CLOSE_RESPONSE = "Yes";
    private final static String CANCEL_CLOSE_RESPONSE = "No";
    private final static String SAVE_CLOSE_RESPONSE = "Save Changes and Close";
    private final PropertyChangeSupport myPCS;
    private JMenuItem myStartMItem;
    private JMenuItem myStopMItem;
    private JMenuItem mySaveMItem;
    private JMenuItem myClearMItem;
    private JButton myStartToolbarButton;
    private JButton myStopToolbarButton;
    private JButton mySaveToolbarButton;
    private JButton myClearToolbarButton;

    /**
     * Constructor for the main GUI of the program. Displays a window that users can use to control the program.
     * If the window is closed while there are unsaved changes to the database, a confirmation window will appear
     * asking the user if they want to close without saving, cancel closing the program, or save the unsaved changes
     * and then close.
     * @param thePcs The property change support that action listeners should be added to.
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

        //<editor-fold desc="Code that controls what happens when the user tries closing the window with unsaved changes">
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // if there are rows in the log table
                if (mySaveMItem.isEnabled()
                        && myClearMItem.isEnabled()
                        && mySaveToolbarButton.isEnabled()
                        && myClearToolbarButton.isEnabled()) {
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
        //</editor-fold>

        setLayout(new BorderLayout());
        initMenuBar();
        initToolBar();
        initFrames();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileItem = new JMenu("File");
        fileItem.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileItem);
        JMenu helpItem = new JMenu("Help");
        helpItem.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpItem);

        myStartMItem = new JMenuItem("Start Logging");
        addActionToStartButtons(myStartMItem);
        myStartMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK));
        fileItem.add(myStartMItem);

        myStopMItem = new JMenuItem("Stop Logging");
        addActionToStopButtons(myStopMItem);
        myStopMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK));
        myStopMItem.setEnabled(false);
        fileItem.add(myStopMItem);

        fileItem.addSeparator();

        mySaveMItem = new JMenuItem("Save Log");
        mySaveMItem.addActionListener(theE -> {
            setUpSaveDatabase();
        });
        mySaveMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        mySaveMItem.setEnabled(false);
        fileItem.add(mySaveMItem);

        myClearMItem = new JMenuItem("Clear Log");
        myClearMItem.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.CLEAR_LOG, null, null);
            disableSaveClearButtons();
        });
        myClearMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        myClearMItem.setEnabled(false);
        fileItem.add(myClearMItem);

        fileItem.addSeparator();

        JMenuItem openDBFrameMItem = new JMenuItem("Query Database (Opens another window)");
        openDBFrameMItem.addActionListener(e -> new DBFrame(myPCS).setVisible(true));
        openDBFrameMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        fileItem.add(openDBFrameMItem);

        JMenuItem clearDBMItem = new JMenuItem("Clear Database");
        clearDBMItem.addActionListener(theE -> {
            setUpClearDatabase();
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

    private void setUpSaveDatabase() {
        myPCS.firePropertyChange(ViewProperties.SAVE_LOG, null, null);
        disableSaveClearButtons();
    }

    private void setUpClearDatabase() {
        myPCS.firePropertyChange(ViewProperties.CLEAR_DATABASE, null, null);
        JOptionPane.showMessageDialog(this, "Database cleared");
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

        mySaveToolbarButton = new JButton(new ImageIcon("src/resources/saveIcon.png"));
        mySaveToolbarButton.setToolTipText("Save the current watched files log to the database.");
        mySaveToolbarButton.addActionListener(theE -> {
            setUpSaveDatabase();
        });
        mySaveToolbarButton.setEnabled(false);

        myClearToolbarButton = new JButton(new ImageIcon("src/resources/clearIcon.png"));
        myClearToolbarButton.setToolTipText("Clear the current watched files log without saving to the database.");
        myClearToolbarButton.addActionListener(theE -> {
            myPCS.firePropertyChange(ViewProperties.CLEAR_LOG, null, null);
            disableSaveClearButtons();
        });
        myClearToolbarButton.setEnabled(false);

        JButton openDatabaseWindowButton = new JButton(new ImageIcon("src/resources/databaseIcon.png"));
        openDatabaseWindowButton.setToolTipText("Open the database query window.");
        openDatabaseWindowButton.addActionListener(theE -> new DBFrame(myPCS).setVisible(true));

        JButton clearDatabaseButton = new JButton(new ImageIcon("src/resources/clearDatabaseIcon.png"));
        clearDatabaseButton.setToolTipText("Clear all rows in the database.");
        clearDatabaseButton.addActionListener(theE -> {
            setUpClearDatabase();
        });

        toolBar.add(myStartToolbarButton);
        toolBar.add(myStopToolbarButton);
        toolBar.add(mySaveToolbarButton);
        toolBar.add(myClearToolbarButton);
        toolBar.add(openDatabaseWindowButton);
        toolBar.add(clearDatabaseButton);

        this.add(toolBar, BorderLayout.NORTH);
    }

    private void initFrames() {
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

    private void disableSaveClearButtons() {
        mySaveMItem.setEnabled(false);
        mySaveToolbarButton.setEnabled(false);
        myClearMItem.setEnabled(false);
        myClearToolbarButton.setEnabled(false);
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
            case ModelProperties.LOG_LIST_MODEL_UPDATED:
                mySaveMItem.setEnabled(true);
                mySaveToolbarButton.setEnabled(true);
                myClearMItem.setEnabled(true);
                myClearToolbarButton.setEnabled(true);
            default:
                break;
        }
    }

    public PropertyChangeSupport getPCS() {
        return myPCS;
    }
}
