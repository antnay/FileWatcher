package model;

import javax.swing.table.DefaultTableModel;

import java.beans.PropertyChangeSupport;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DBFriend {
    private final PropertyChangeSupport myPCS;

    public DBFriend(PropertyChangeSupport thePCS) {
        myPCS = thePCS;
    }

    public void getTableModel(String[] theQuery) {

        DefaultTableModel tableModel = new DefaultTableModel();
        String[] columnNames = {"Filename", "Extension", "Path", "Event", "Timestamp"};
        tableModel.setColumnIdentifiers(columnNames);
        String query = process(theQuery);
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

    private String process(String[] theQ) {
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
