package view;

import javax.swing.table.DefaultTableModel;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.beans.PropertyChangeSupport;
import javax.swing.JLabel;
import java.awt.FlowLayout;



public class DBFrame extends JFrame {

    private JTable databaseRecords;
    private JTextField extensionQueryField;
    private JButton submitButton;
    private final PropertyChangeSupport myPCS;

    public DBFrame(PropertyChangeSupport pcs) {
        this.myPCS = pcs;
        setTitle("Query Form");
        setSize(900, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setUIComponents();

        myPCS.addPropertyChangeListener("Query Results", evt -> {
            DefaultTableModel tableModel = (DefaultTableModel) evt.getNewValue();
            updateTable(tableModel);
        });
    }

    private void setUIComponents() {

        JPanel queryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel queryLabel = new JLabel("Extension Query: (Empty = ALL files in database)");
        queryPanel.add(queryLabel);

        extensionQueryField = new JTextField(15);
        queryPanel.add(extensionQueryField);

        submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> runSearchQuery());
        queryPanel.add(submitButton);

        add(queryPanel, BorderLayout.NORTH);

        databaseRecords = new JTable(new DefaultTableModel());
        JScrollPane tableScrollPane = new JScrollPane(databaseRecords);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private void runSearchQuery() {
        String extension = extensionQueryField.getText().trim();
        String query = extension.isEmpty() ? "SELECT * FROM event_log" :
                "SELECT * FROM event_log WHERE extension = '" + extension + "'";

        myPCS.firePropertyChange("Execute Query", null, query);
    }

    public void updateTable(DefaultTableModel tableModel) {
        databaseRecords.setModel(tableModel);
    }
}
