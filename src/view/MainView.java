package view;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class MainView extends JFrame implements PropertyChangeListener {

    Map<String, JMenuItem> menuItemM = new HashMap<>();

    public MainView() {
        setTitle("FileWatcher");
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initMenuBar();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileItem = new JMenu("File");
        menuBar.add(fileItem);
        fileItem.add(createMenuItem("Start", theE -> System.out.println("start stop"),
                Optional.of("startStop"), Optional.of(KeyEvent.VK_ENTER))); // change state once clicked to display stop
        // fileItem.add(createMenuItem("stop", null, KeyEvent.VK_S));
        fileItem.add(createMenuItem("Save", theE -> System.out.println("save"),
                Optional.empty(), Optional.of(KeyEvent.VK_S)));

        JMenu helpItem = new JMenu("Help");
        menuBar.add(helpItem);
        helpItem.add(createMenuItem("About", theE -> System.out.println("about"),
                Optional.empty(), Optional.empty()));
        helpItem.add(createMenuItem("Show Shortcuts", theE -> System.out.println("shortcuts"),
                Optional.of("shortcuts"), Optional.of(KeyEvent.VK_K)));

        setJMenuBar(menuBar);
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param theEvent A PropertyChangeEvent object describing the event source
     *                 and the property that has changed.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        switch (theEvent.getPropertyName()) {
            // case SOME_EVENT:
            default:
        }
    }

    /**
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
        menuItemM.put((theName.isPresent()) ? theName.get() : theText, menuItem);
        return menuItem;
    }

}
