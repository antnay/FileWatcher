package view;


//import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import javax.swing.*;
import java.sql.*;
import java.awt.*;
//import model.DBManager; //Show we make model.DBManager public to be able to import here

// query and view db
public class DBFrame extends JFrame {

    // Need a JTable to display the results of a query, a text field to put queries, and a button to run queries
    private JTable databaseRecords;
    private JTextField enteringQueries;
    private JButton searchQueryExecuteButton;

    public DBFrame () {
        setTitle("Database Query Viewer");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); //Close the DBFrame only
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setUIComponents();
    }

    private void setUIComponents() {
        //The input text field for Queries
        enteringQueries = new JTextField(20);
        enteringQueries.setText("Write Queries Here");

        //The button to execute the Query
        searchQueryExecuteButton = new JButton("Execute Query");
        searchQueryExecuteButton.addActionListener(e -> runSearchQuery());

        JPanel queryPanel = new JPanel();
        queryPanel.add(enteringQueries);
        queryPanel.add(searchQueryExecuteButton);
        add(queryPanel, BorderLayout.NORTH);

        //Table for displaying Query results
        databaseRecords = new JTable(new DefaultTableModel());
        add(new JScrollPane(databaseRecords), BorderLayout.CENTER); //User is able to Scroll
    }

    private void runSearchQuery() {

        //Finish this part

     /*
     catch (SQLException TheE) {
     // Display any error that is happening
        JOptionPane.showMessageDialog(this, "SQL Error: " + TheE.getMessage(), "Query Error", JOptionPane.ERROR_MESSAGE);
        }
      */

    }
    }

