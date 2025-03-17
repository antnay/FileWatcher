package model;

import javax.swing.table.DefaultTableModel;

import java.beans.PropertyChangeSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


public class DBFriend {
    private final PropertyChangeSupport myPCS;
    private final Email myEmail;
    private String currentQuery;

    public DBFriend(PropertyChangeSupport thePCS) {
        myPCS = thePCS;
        currentQuery = "SELECT * FROM event_log";
        myEmail = new Email();
    }

    public void getTableModel(String[] theQuery) {
        DefaultTableModel tableModel = new DefaultTableModel();
        String[] columnNames = {"Filename", "Extension", "Path", "Event", "Timestamp"};
        tableModel.setColumnIdentifiers(columnNames);
        String query = processQuery(theQuery);
        try (ResultSet resultSet = query(query)) {
            if (resultSet != null) {
                while (resultSet.next()) {
                    Object[] rowData = new Object[columnNames.length];
                    rowData[0] = resultSet.getString("filename");
                    rowData[1] = resultSet.getString("extension");
                    rowData[2] = resultSet.getString("path");
                    rowData[3] = resultSet.getString("event");
                    rowData[4] = resultSet.getString("timestamp");
                    tableModel.addRow(rowData);
                }
            }
            myPCS.firePropertyChange(ModelProperties.TABLE_MODEL_QUERY, null, tableModel);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendEmail(String theEmail) {
        if (!Email.gCheck()) {
            System.err.println("Unable to send email, credentials are not setup.");
            return;
        }
        File csv = generateCSV();
        System.out.println("oh man we're sending email");
        myEmail.sendEmailWithLogFile(theEmail, csv);
    }

    private File generateCSV() {
        File logFile = new File("database/file_watcher_log.csv");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            ResultSet resSet = DBManager.getDBManager().executeQuery(currentQuery);
            ResultSetMetaData metaData = resSet.getMetaData();
            int colNum = metaData.getColumnCount();
            for (int i = 1; i < colNum; i++) {
                writer.append('"').append(metaData.getColumnName(i)).write("\",");
            }
            writer.append('"').append(metaData.getColumnName(colNum)).append("\"\n");
            while (resSet.next()) {
                for (int i = 1; i < colNum; i++) {
                    writer.append('"').append(resSet.getString(i)).write("\",");
                }
                writer.append('"').append(resSet.getString(colNum)).write("\"\n");
            }
            writer.close();
        } catch (DatabaseException | SQLException | IOException theE) {
            System.err.println("Error writing CSV: " + theE.getMessage());
        }
        return logFile;
    }

    private String processQuery(String[] theQ) {
        StringBuilder sB = new StringBuilder("SELECT filename, extension, path, event, timestamp FROM event_log WHERE 1=1 ");
        if (!theQ[0].isEmpty()) {
            sB.append("AND filename LIKE '%").append(theQ[0]).append("%'");
        }
        if (!theQ[1].isEmpty()) {
            sB.append("AND extension LIKE '%").append(theQ[1]).append("%'");
        }
        if (!theQ[2].isEmpty()) {
            sB.append("AND path LIKE '%").append(theQ[2]).append("%'");
        }
        switch (theQ[3]) {
            case "CREATE" -> sB.append("AND event='").append("ENTRY_CREATE").append("'");
            case "MODIFY" -> sB.append("AND event='").append("ENTRY_MODIFY").append("'");
            case "DELETE" -> sB.append("AND event='").append("ENTRY_DELETE").append("'");
        }
        if (!theQ[4].isEmpty() && !theQ[5].isEmpty()) {
            sB.append("AND timestamp BETWEEN '").append(theQ[4]).append("' AND '").append(theQ[5]).append("'");
        } else if (!theQ[4].isEmpty()) {
            sB.append("AND timestamp >= '").append(theQ[4]).append("'");
        } else if (!theQ[5].isEmpty()) {
            sB.append("AND timestamp <= '").append(theQ[5]).append("'");
        }
        System.out.println(sB.toString());
        currentQuery = sB.toString();
        return sB.toString();
    }

    private ResultSet query(String theQuery) {
        try {
            return DBManager.getDBManager().executeQuery(theQuery);
        } catch (DatabaseException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }


}
