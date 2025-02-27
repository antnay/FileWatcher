package model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.table.DefaultTableModel;

final class DBManager {

    private static final DBManager DB_INSTANCE = new DBManager();

    private Connection connection;

    private DBManager() {
        connection = null;
    }

    static DBManager getDBManager() {
        return DB_INSTANCE;
    }

    /**
     * Checks connection to database.
     *
     * @return true if there is a connection to the database, false otherwise.
     */
    boolean isConnected() {
        return connection != null;
    }

    void connect() throws DatabaseException {
        File dbPath = new File("database/log.sql");
        if (!Files.exists(Path.of(dbPath.getAbsolutePath()))) {
            try {
                Files.createDirectories(Path.of(dbPath.getParentFile().getCanonicalPath()));
                Files.createFile(Path.of(dbPath.getAbsolutePath()));
            } catch (IOException theE) {
                throw new RuntimeException("Error creating database", theE);
            }
        }
        String dbUrl = "jdbc:sqlite:" + dbPath.getAbsolutePath();
        try {
            connection = DriverManager.getConnection(dbUrl);
            System.out.println("Connected to database");
            initDB();
        } catch (SQLException theE) {
            throw new DatabaseException("Error connecting to database", theE);
        }
    }

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

    void disconnect() throws DatabaseException {
        try {
            if (isConnected()) {
                connection.close();
                connection = null;
            }
            System.out.println("Disconnected from database");
        } catch (SQLException theE) {
            throw new DatabaseException("Error disconnecting from database", theE);
        }
    }

    void clearTable() throws DatabaseException {
        try {
            Statement statement = connection.createStatement();
            statement.execute("DELETE FROM eventlog");
        } catch (SQLException theE) {
            throw new DatabaseException("Error clearing table", theE);
        }
    }

    void addEvent(Event theEvent) throws DatabaseException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO
                event_log_temp (extension, filename, path, event, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, theEvent.getMyExtension());
            statement.setString(2, theEvent.getFileName());
            statement.setString(3, theEvent.getPath());
            statement.setString(4, theEvent.geEventKind());
            statement.setString(5, theEvent.getTimeStamp());
            statement.execute();
        } catch (SQLException theE) {
            throw new DatabaseException("Error adding event to database", theE);
        }
    }

    void mergeTempEvents() throws DatabaseException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("""
                    INSERT INTO
                    event_log (extension, filename, path, event, timestamp)
                    SELECT extension, filename, path, event, timestamp
                    FROM event_log_temp
                    """);
            statement.executeQuery("DELETE FROM event_log_temp");
        } catch (SQLException theE) {
            throw new DatabaseException("Error adding events to database", theE);
        }

    }

    void initDB() throws DatabaseException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to database");
        }
        try (Statement statement = connection.createStatement()) {
            ResultSet res = statement.executeQuery(
                    "SELECT * FROM sqlite_master WHERE type='table' AND name='event_log';");
            if (!res.next()) {
                statement.executeUpdate("""
                        CREATE TABLE "event_log" (
                        \t"id"\tINTEGER NOT NULL UNIQUE,
                        \t"extension"\tTEXT,
                        \t"filename"\tTEXT,
                        \t"path"\tTEXT,
                        \t"event"\tTEXT,
                        \t"timestamp"\tDATETIME,
                        \tPRIMARY KEY("id" AUTOINCREMENT)
                        );""");
            }
            res = statement.executeQuery(
                    "SELECT * FROM sqlite_master WHERE type='table' AND name='event_log_temp';");
            if (!res.next()) {
                statement.executeUpdate("""
                        CREATE TABLE "event_log_temp" (
                        \t"extension"\tTEXT,
                        \t"filename"\tTEXT,
                        \t"path"\tTEXT,
                        \t"event"\tTEXT,
                        \t"timestamp"\tDATETIME
                        );""");
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
        } catch (SQLException theE) {
            throw new RuntimeException("Error initializing database", theE);
        }
    }

}
