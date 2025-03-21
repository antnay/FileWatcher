package view;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import model.ModelProperties;

/**
 * An element of the GUI that shows the user a log of changes that were caught by the watcher system.
 */
class LogPanel extends JPanel implements PropertyChangeListener {
    /**
     * The JTable displayed in this panel.
     */
    private final JTable myJTable;

    /**
     * Constructs a panel that displays a log of changes caught by the watcher system in a table.
     * When notified of the table model being changed, the displayed table will update.
     *
     * @param thePcs The <code>PropertyChangeSupport</code> that this listener should be added to.
     */
    public LogPanel(PropertyChangeSupport thePcs) {
        thePcs.addPropertyChangeListener(this);

        myJTable = new JTable(new DefaultTableModel());
        myJTable.setEnabled(false);
        JScrollPane tableContainer = new JScrollPane(myJTable);

        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }

    /**
     * This method gets called when a property bound in a
     * <code>PropertyChangeSupport</code> this listener is added to is changed.
     *
     * @param theEvent A PropertyChangeEvent object describing the event source
     *          and the property that has changed.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent theEvent) {
        if (theEvent.getPropertyName().equals(ModelProperties.LOG_LIST_MODEL_UPDATED)) {
            myJTable.setModel((TableModel) theEvent.getNewValue());
        }
    }
}
