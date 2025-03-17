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
import model.FileListModel;
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
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DBFrame extends JFrame {

    private static String DATE_PLACE_HOLDER = "yyyy-mm-dd";
    private final PropertyChangeSupport myPCS;
    private JTable mySearchTable;
    private JTextField myExtensionField;
    private JButton mySubmitButton;
    private JButton myExportButton;
    private JTextField myPathField;
    private JTextField myFileField;
    private JTextField myDateStartField;
    private JTextField myDateEndField;
    private JTextField myEmailField;
    private JComboBox<String> myEventField;

    public DBFrame(PropertyChangeSupport pcs) {
        this.myPCS = pcs;
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
        mySubmitButton.addActionListener(_ -> runSearchQuery());
        setFocusable(true);
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
        queries[4] = startDate.equals(DATE_PLACE_HOLDER) ? "" : startDate;
        queries[5] = endDate.equals(DATE_PLACE_HOLDER) ? "" : endDate;

        myPCS.firePropertyChange(ViewProperties.DB_QUERY, null, queries);
    }

    public void updateTable(DefaultTableModel tableModel) {
        mySearchTable.setModel(tableModel);
        checkForInput();
    }

    private void addPlaceholder(JTextField textField) {
        textField.setText(DATE_PLACE_HOLDER);
        textField.setForeground(Color.GRAY);
        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent theE) {
                if (textField.getText().equals(DATE_PLACE_HOLDER)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent theE) {
                String text = textField.getText().trim();
                if (text.isEmpty()) {
                    textField.setText(DATE_PLACE_HOLDER);
                    textField.setForeground(Color.GRAY);
                } else if (!isValidDate(text)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }
        });
    }

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

    private void export() {
        myPCS.firePropertyChange(ViewProperties.EMAIL, null, myEmailField.getText().strip());
    }

    private KeyAdapter addListeners(JButton theButton) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    theButton.doClick();
                }
            }
        };
    }

    private void checkForInput() {
        String email = myEmailField.getText().strip();
        myExportButton.setEnabled(!email.isEmpty() && mySearchTable.getRowCount() != 0);
    }

    private class InputDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            checkForInput();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            checkForInput();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            checkForInput();
        }
    }
}
