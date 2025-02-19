package model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.table.DefaultTableModel;
import java.sql.*;


final class DBManager {

    private static final DBManager DB_INSTANCE = new DBManager();

    private Connection connection;

    private DBManager() {
        connection = null;
        // TODO: Maybe call connect() right away?
    }

    static DBManager getDBManager() {
        return DB_INSTANCE;
    }

    /**
     * Checks connection to database.
     *
     * @return true if there is a connection to the database, false otherwise.
     */
    public boolean isConnected() {
        return connection != null;
    }

    void connect() {
        File dbPath = new File("database/log.sql");
        if (!Files.exists(Path.of(dbPath.getAbsolutePath()))) {
            try {
                Files.createDirectories(Path.of(dbPath.getParentFile().getCanonicalPath()));
                Files.createFile(Path.of(dbPath.getAbsolutePath()));
            } catch (IOException theE) {
                System.err.println("Error creating database " + dbPath);
                System.exit(1);
            }
        }
        String dbUrl = "jdbc:sqlite:" + dbPath.getAbsolutePath();
        try {
            connection = DriverManager.getConnection(dbUrl);
            System.out.println("Connected to database");
            initDB();
        } catch (SQLException theE) {
            connection = null;
            System.err.println("Error connecting to database " + theE.getMessage());
            // TODO: Handle this error
            //theE.printStackTrace(); (If we want to log what the exact error is)
        }
    }

    //Work on this more, check how to make it better or if it is the right way
    public DefaultTableModel executeQuery(String query) {
        DefaultTableModel tableModel = new DefaultTableModel();

        if (!isConnected()) {
            System.err.println("Database is not connected!");
            return tableModel; // Return empty model if no connection
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Retrieve column names dynamically
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            String[] columnNames = new String[columnCount];

            for (int i = 1; i <= columnCount; i++) {
                columnNames[i - 1] = metaData.getColumnName(i);
            }
            tableModel.setColumnIdentifiers(columnNames);

            // Populate table model with query results
            while (rs.next()) {
                Object[] rowData = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    rowData[i - 1] = rs.getObject(i);
                }
                tableModel.addRow(rowData);
            }

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }

        return tableModel;
    }


    void disconnect() {
        try {
            if (!isConnected()) {
                connection.close();
                connection = null;
            }
            System.out.println("Disconnected from database");
        } catch (SQLException theE) {
            System.err.println("Error closing database: " + theE.getMessage());
            // TODO: Handle this error
            //theE.printStackTrace();
        }
    }

    void clearTable() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("DELETE FROM eventlog");
        } catch (SQLException theE) {
            // TODO: Handle this error
            System.err.println("Error clearing table: " + theE.getMessage());
            // theE.printStackTrace();
        }
    }

    void addEvent(Event theEvent) {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO
                eventlog(extension, filename, path, event, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, theEvent.getMyExtension());
            statement.setString(2, theEvent.getFileName());
            statement.setString(3, theEvent.getPath());
            statement.setString(4, theEvent.geEventKind());
            statement.setString(5, theEvent.getTimeStamp());
            statement.execute();
        } catch (SQLException e) {
            // TODO: Handle this error
            System.err.println("Error adding event to database: " + e.getMessage());
            //e.printStackTrace();
        }
    }

    void initDB() {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        try (Statement statement = connection.createStatement()) {
            ResultSet res = statement.executeQuery(
                    "SELECT * FROM sqlite_master WHERE type='table' AND name='eventlog';");
            if (!res.next()) {
                statement.executeUpdate("""
                        CREATE TABLE "eventlog" (
                        \t"id"\tINTEGER NOT NULL UNIQUE,
                        \t"extension"\tTEXT,
                        \t"filename"\tTEXT,
                        \t"path"\tTEXT,
                        \t"event"\tTEXT,
                        \t"timestamp"\tDATETIME,
                        \tPRIMARY KEY("id" AUTOINCREMENT)
                        );""");
                System.out.println("added table");
            }
            // res = statement.executeQuery(
            // "SELECT name FROM sqlite_master WHERE type='table' AND name='users';"
            // );
            // if (!res.next()) {
            // statement.executeUpdate("""
            // CREATE TABLE "eventlog" (
            // \t"id"\tINTEGER NOT NULL UNIQUE,
            // \t"username"\tTEXT UNIQUE,
            // \t"password"\tTEXT, // hash me
            // \t"timestamp"\tDATETIME DEFAULT DATETIME,
            // \tPRIMARY KEY("id" AUTOINCREMENT)
            // );""");
            // System.out.println("configured DB");
            // }
        } catch (SQLException e) {
            throw new RuntimeException(e);
            // TODO: Handle this error
        }
    }


}
