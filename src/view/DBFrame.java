package view;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.ModelProperties;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeSupport;

public class DBFrame extends JDialog {

    private final PropertyChangeSupport myPCS;
    private JTable myDatabaseRecords;
    private JTextField myExtensionField;
    private JButton mySubmitButton;
    private JTextField myPathField;
    private JTextField myFileField;
    private JTextField myDateStartField;
    private JTextField myDateEndField;
    private JComboBox<String> myEventField;

    public DBFrame(PropertyChangeSupport pcs) {
        this.myPCS = pcs;
        setTitle("Database Search");
        setModal(true);
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

    /*
     * General Search
     * Filename
     * Extension
     * Event
     * Date range
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
        JLabel extensionLabel = new JLabel("Extension:");
        queryPanel.add(extensionLabel, gbc);
        gbc.gridy++;
        myExtensionField = new JTextField(15);
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
        queryPanel.add(myDateStartField, gbc);

        gbc.gridy++;
        JLabel dateEndLabel = new JLabel("Date End:");
        queryPanel.add(dateEndLabel, gbc);
        gbc.gridy++;
        myDateEndField = new JTextField(15);
        queryPanel.add(myDateEndField, gbc);

        gbc.gridy++;
        mySubmitButton = new JButton("Submit");
        mySubmitButton.addActionListener(_ -> runSearchQuery());
        queryPanel.add(mySubmitButton, gbc);

        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(queryPanel, BorderLayout.NORTH);

        add(containerPanel, BorderLayout.WEST);

        myDatabaseRecords = new JTable(new DefaultTableModel());
        JScrollPane tableScrollPane = new JScrollPane(myDatabaseRecords);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private void runSearchQuery() {
        String[] queries = new String[6];
        String filename = myFileField.getText().trim();
        String extension = myExtensionField.getText().trim();
        String path = myPathField.getText().trim();
        String event = String.valueOf(myEventField.getSelectedItem());
        String startDate = myDateStartField.getText().trim();
        String endDate = myDateEndField.getText().trim();
        queries[0] = filename.isEmpty() ? "" : filename;
        queries[1] = extension.isEmpty() ? "" : extension;
        queries[2] = path.isEmpty() ? "" : path;
        queries[3] = event;
        queries[4] = startDate.isEmpty() ? "" : startDate;
        queries[5] = endDate.isEmpty() ? "" : endDate;

        myPCS.firePropertyChange(ViewProperties.DB_QUERY, null, queries);
    }

    public void updateTable(DefaultTableModel tableModel) {
        myDatabaseRecords.setModel(tableModel);
    }
}
