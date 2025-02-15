package view;

import javax.swing.table.DefaultTableModel;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import controller.DBController;

// query and view db
public class DBFrame extends JFrame {

    // Need a JTable to display the results of a query, a text field to put queries in, and a button to run queries
    private JTable databaseRecords;
    private JTextField enteringQueries;
    private JButton searchQueryExecuteButton;
    private DBController dbController; //To handle database queries

    public DBFrame () {
        setTitle("SQL Query Executor");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); //Close the DBFrame only
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setUIComponents();
    }

    //This is where we set up and initialize all the UI components for the DBFrame
    private void setUIComponents() {

        //The text field for Queries
        enteringQueries = new JTextField(40);
        enteringQueries.setText("Write SQL Queries Here");

        //The button to execute the Query
        searchQueryExecuteButton = new JButton("Run Query");
        searchQueryExecuteButton.addActionListener(e -> runSearchQuery());

        //The Plane to hold the query text field and the run button
        JPanel queryPanel = new JPanel();
        queryPanel.add(enteringQueries);
        queryPanel.add(searchQueryExecuteButton);
        add(queryPanel, BorderLayout.NORTH);

        //Table for displaying Query results
        databaseRecords = new JTable(new DefaultTableModel());
        add(new JScrollPane(databaseRecords), BorderLayout.CENTER); // Allows users to scroll when the table contains more data than can fit in the DBFrame
    }

    //Gets the SQL query that the user entered
    public String obtainSearchQuery() {
        return enteringQueries.getText().trim();
    }

    private void runSearchQuery() {
        String query = obtainSearchQuery();
        //DefaultTableModel tableModel = dbController.executeQuery(query); (Will uncomment it once I resolve it in the DBController)
        //databaseRecords.setModel(tableModel);

    }
    }

