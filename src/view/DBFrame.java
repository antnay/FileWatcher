package view;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import model.ModelProperties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;

/**
 * A window that allows users to query the database. Also allows users to send
 * an email of a .csv file representing the query results.
 */
public class DBFrame extends JFrame {

    /**
     * Constant string to use as placeholder text for date query input field to show expected format.
     */
    private final static String DATE_PLACE_HOLDER = "yyyy-mm-dd";

    /**
     * <code>PropertyChangeSupport</code> for notifying listeners of events.
     */
    private final PropertyChangeSupport myPCS;

    /**
     * The JTable displayed in this panel.
     */
    private JTable mySearchTable;

    /**
     * JButton to let users submit the query criteria and search the database.
     */
    private JButton mySubmitButton;

    /**
     * JButton to let users email a .csv representation of the query results.
     */
    private JButton myExportButton;

    /**
     * JTextField to let users query for a specific file name.
     */
    private JTextField myFileField;

    /**
     * JComboBox to let users query for a specific file extension. Provides a small list of extensions.
     */
    private JComboBox<String> myExtensionField;

    /**
     * JTextField to let users query for a specific path.
     */
    private JTextField myPathField;

    /**
     * JComboBox to let users query for a specific file change event. Provides a list of file change events.
     */
    private JComboBox<String> myEventField;

    /**
     * JTextField to let users query for entries after a specific date.
     */
    private JTextField myDateStartField;

    /**
     * JTextField to let users query for entries before a specific date.
     */
    private JTextField myDateEndField;

    /**
     * JTextField to let users provide an email address to send the .csv file to.
     */
    private JTextField myEmailField;

    /**
     * Constructs a window that allows the user to query the database
     * and send the query results as a .csv file through email.
     *
     * @param thePcs The <code>PropertyChangeSupport</code> that this listener should be added to.
     */
    public DBFrame(final PropertyChangeSupport thePcs) {
        this.myPCS = thePcs;
        setTitle("Database Search");
        setSize(900, 600);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setUIComponents();

        myPCS.addPropertyChangeListener(ModelProperties.TABLE_MODEL_QUERY, evt -> {
            DefaultTableModel tableModel = (DefaultTableModel) evt.getNewValue();
            updateTable(tableModel);
        });
    }

    /**
     * Helper method to set up the input fields and buttons to allow the user to query the database.
     */
    private void setUIComponents() {
        JPanel queryPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        queryPanel.setPreferredSize(new Dimension(200, getHeight()));

        JLabel fileLabel = new JLabel("Filename:");
        queryPanel.add(fileLabel, gbc);
        gbc.gridy++;
        myFileField = new JTextField(15);
        queryPanel.add(myFileField, gbc);
        gbc.gridy++;

        String[] commonExtensions = {
                ".txt",
                ".exe",
                ".jar"
        };

        JLabel extensionLabel = new JLabel("Extension:");
        queryPanel.add(extensionLabel, gbc);
        gbc.gridy++;
        myExtensionField = new JComboBox<>(commonExtensions);
        myExtensionField.setEditable(true);
        myExtensionField.setSelectedItem(""); // default selection is blank instead of first option in menu
        queryPanel.add(myExtensionField, gbc);
        gbc.gridy++;

        JLabel pathLabel = new JLabel("Path:");
        queryPanel.add(pathLabel, gbc);
        gbc.gridy++;
        myPathField = new JTextField(15);
        queryPanel.add(myPathField, gbc);
        gbc.gridy++;

        JLabel eventLabel = new JLabel("Event:");
        queryPanel.add(eventLabel, gbc);
        gbc.gridy++;
        String[] commonEvents = {"ALL", "CREATE", "MODIFY", "DELETE"};
        myEventField = new JComboBox<>(commonEvents);
        queryPanel.add(myEventField, gbc);
        gbc.gridy++;

        JLabel dateStartLabel = new JLabel("Date Start:");
        queryPanel.add(dateStartLabel, gbc);
        gbc.gridy++;
        myDateStartField = new JTextField(15);
        addPlaceholder(myDateStartField);
        queryPanel.add(myDateStartField, gbc);
        gbc.gridy++;

        JLabel dateEndLabel = new JLabel("Date End:");
        queryPanel.add(dateEndLabel, gbc);
        gbc.gridy++;
        myDateEndField = new JTextField(15);
        addPlaceholder(myDateEndField);
        queryPanel.add(myDateEndField, gbc);
        gbc.gridy++;

        mySubmitButton = new JButton("Submit");
        mySubmitButton.addActionListener(theE -> runSearchQuery());
        queryPanel.add(mySubmitButton, gbc);
        gbc.gridy++;

        myFileField.addKeyListener(addListeners(mySubmitButton));
        myExtensionField.addKeyListener(addListeners(mySubmitButton));
        myPathField.addKeyListener(addListeners(mySubmitButton));
        myEventField.addKeyListener(addListeners(mySubmitButton));
        myDateStartField.addKeyListener(addListeners(mySubmitButton));
        myDateEndField.addKeyListener(addListeners(mySubmitButton));

        JPanel exportPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcExport = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.SOUTH;

        gbcExport.gridy++;
        JLabel emailLabel = new JLabel("Email:");
        exportPanel.add(emailLabel, gbcExport);
        gbcExport.gridy++;
        myEmailField = new JTextField(15);
        myEmailField.getDocument().addDocumentListener(new InputDocumentListener());
        exportPanel.add(myEmailField, gbcExport);
//        gbcExport.gridx++;
        gbcExport.gridy++;
        myExportButton = new JButton("Export");
        myExportButton.setEnabled(false);
        myExportButton.addActionListener(_ -> {
            export();
        });
        myEmailField.addKeyListener(addListeners(myExportButton));
        exportPanel.add(myExportButton, gbcExport);


        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(queryPanel, BorderLayout.CENTER);
        containerPanel.add(exportPanel, BorderLayout.SOUTH);

        add(containerPanel, BorderLayout.WEST);

        mySearchTable = new JTable(new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        mySearchTable.setRowSelectionAllowed(true);
        mySearchTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane tableScrollPane = new JScrollPane(mySearchTable);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    /**
     * Helper method to get the data from the input fields and run the query against the database.
     */
    private void runSearchQuery() {
        String[] queries = new String[6];
        String filename = myFileField.getText().trim();
        String extension = Objects.requireNonNull(myExtensionField.getSelectedItem()).toString().trim();
        String path = myPathField.getText().trim();
        String event = String.valueOf(myEventField.getSelectedItem());
        String startDate = myDateStartField.getText().trim();
        String endDate = myDateEndField.getText().trim();
        queries[0] = filename.isEmpty() ? "" : filename;
        queries[1] = extension.isEmpty() ? "" : extension;
        queries[2] = path.isEmpty() ? "" : path;
        queries[3] = event;
        queries[4] = startDate.equals(DATE_PLACE_HOLDER) ? "" : startDate;
        queries[5] = endDate.equals(DATE_PLACE_HOLDER) ? "" : endDate;

        myPCS.firePropertyChange(ViewProperties.DB_QUERY, null, queries);
    }

    /**
     * Helper method to display the query results to the table
     * and enable the email export button when appropriate.
     *
     * @param tableModel The <code>DefaultTableModel</code> to display in the table.
     */
    private void updateTable(DefaultTableModel tableModel) {
        mySearchTable.setModel(tableModel);
        checkForInput();
    }

    /**
     * Helper method that adds placeholder text to the given <code>JTextField</code>.
     * Text will only be visible when there is no text in the field and it does not have focus.
     *
     * @param theTextField The <code>JTextField</code> to add placeholder text to.
     */
    private void addPlaceholder(final JTextField theTextField) {
        theTextField.setText(DATE_PLACE_HOLDER);
        theTextField.setForeground(Color.GRAY);
        theTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent theE) {
                if (theTextField.getText().equals(DATE_PLACE_HOLDER)) {
                    theTextField.setText("");
                    theTextField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent theE) {
                String text = theTextField.getText().trim();
                if (text.isEmpty()) {
                    theTextField.setText(DATE_PLACE_HOLDER);
                    theTextField.setForeground(Color.GRAY);
                } else if (!isValidDate(text)) {
                    theTextField.setText("");
                    theTextField.setForeground(Color.BLACK);
                }
            }
        });
    }

    /**
     * Helper method to validate that the date input is in the expected format yyyy-mm-dd.
     *
     * @param date The input to validate is in the expected date format.
     * @return <code>true</code> if the given string can be parsed as a simple date format, <code>false</code> otherwise.
     */
    private boolean isValidDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            sdf.parse(date);
            return true;
        } catch (ParseException theE) {
            return false;
        }
    }

    /**
     * Helper method to add functionality to the export button.
     */
    private void export() {
        myPCS.firePropertyChange(ViewProperties.EMAIL, null, myEmailField.getText().strip());
    }

    /**
     * Helper method to create a KeyAdapter to map an "Enter" key press to a button click.
     *
     * @param theButton The <code>JButton</code> to click when "Enter" is pressed.
     * @return A KeyAdapter for the given button that will listen for the "Enter" key to be pressed.
     */
    private KeyAdapter addListeners(final JButton theButton) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    theButton.doClick();
                }
            }
        };
    }

    /**
     * Helper method to check that there is input in the email field and enabling the export button as appropriate.
     */
    private void checkForInput() {
        String email = myEmailField.getText().strip();
        myExportButton.setEnabled(!email.isEmpty() && mySearchTable.getRowCount() != 0);
    }

    /**
     * Inner class to allow reuse of document listener properties for both input fields.
     */
    private class InputDocumentListener implements DocumentListener {
        /**
         * Gives a notification that something was inserted into the field.
         *
         * @param theE the document event
         */
        @Override
        public void insertUpdate(final DocumentEvent theE) {
            checkForInput();
        }

        /**
         * Gives a notification that something was removed from the field.
         *
         * @param theE the document event
         */
        @Override
        public void removeUpdate(final DocumentEvent theE) {
            checkForInput();
        }

        /**
         * Gives a notification that an attribute of the field changed.
         *
         * @param theE the document event
         */
        @Override
        public void changedUpdate(final DocumentEvent theE) {
            checkForInput();
        }
    }
}
