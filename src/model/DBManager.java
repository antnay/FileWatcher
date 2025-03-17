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
        Path path = Path.of(dbPath.getAbsolutePath());
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(Path.of(dbPath.getParentFile().getCanonicalPath()));
                Files.createFile(path);
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

    public ResultSet executeQuery(String theQuery) throws DatabaseException {
        if (!isConnected()) {
            throw new DatabaseException("Not connected to database");
        }

        try {
            return connection.createStatement().executeQuery(theQuery);
        } catch (SQLException e) {
            throw new DatabaseException("Error executing query", e);
        }
    }

    // ResultSet getTable() throws DatabaseException {
    //     if (!isConnected()) {
    //         throw new DatabaseException("Not connected to database");
    //     }
    //     try {
    //         return connection.createStatement().executeQuery("""
    //                 SELECT * FROM event_log
    //                 """);
    //     } catch (SQLException theE) {
    //         throw new DatabaseException("Error querying database", theE);
    //     }
    // }

    void clearTable() throws DatabaseException {
        if (!isConnected()) {
            throw new DatabaseException("Not connected to database");
        }
        try {
            Statement statement = connection.createStatement();
            statement.execute("DELETE FROM event_log");
        } catch (SQLException theE) {
            throw new DatabaseException("Error clearing table", theE);
        }
    }

    void clearTempTable() throws DatabaseException {
        if (!isConnected()) {
            throw new DatabaseException("Not connected to database");
        }
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM event_log_temp");
        } catch (SQLException theE) {
            throw new DatabaseException("Error clearing table", theE);
        }
    }

    void clearWatchTable() throws DatabaseException {
        if (!isConnected()) {
            throw new DatabaseException("Not connected to database");
        }
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM watch_table");
        } catch (SQLException theE) {
            throw new DatabaseException("Error clearing table", theE);
        }
    }

    void addToWatch(File theFile) throws DatabaseException {
        if (!isConnected()) {
            throw new DatabaseException("Not connected to database");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO
                watch_table (path)
                VALUES (?)
                """)) {
            statement.setString(1, theFile.getAbsolutePath());
            statement.execute();
        } catch (SQLException theE) {
            System.err.println("SQL Error: " + theE.getMessage());
        }

    }

    void addEvent(Event theEvent) throws DatabaseException {
        if (!isConnected()) {
            throw new DatabaseException("Not connected to database");
        }
        // System.out.println("Adding event: " + theEvent);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO
                event_log_temp (extension, filename, path, event)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, theEvent.getExtension());
            statement.setString(2, theEvent.getFileName());
            statement.setString(3, theEvent.getPath());
            statement.setString(4, theEvent.geEventKind());
            // statement.setString(5, theEvent.getTimeStamp().toString());
            statement.execute();
        } catch (SQLException theE) {
            throw new DatabaseException("Error adding event to database", theE);
        }
    }

    void mergeTempEvents() throws DatabaseException {
        if (!isConnected()) {
            throw new DatabaseException("Not connected to database");
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO
                    event_log (extension, filename, path, event, timestamp)
                    SELECT extension, filename, path, event, timestamp
                    FROM event_log_temp
                    """);
            statement.executeUpdate("DELETE FROM event_log_temp");
        } catch (SQLException theE) {
            throw new DatabaseException("Error adding events to database", theE);
        }
    }

    void initDB() throws DatabaseException {
        if (!isConnected()) {
            throw new DatabaseException("Not connected to database");
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
                        \t"timestamp"\tDATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        \tPRIMARY KEY("id" AUTOINCREMENT)
                        );
                        """);
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
                        \t"timestamp"\tDATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                        );
                        """);
            }
            res = statement.executeQuery(
                    "SELECT * FROM sqlite_master WHERE type='table' AND name='watch_table';");
            if (!res.next()) {
                statement.executeUpdate("""
                         CREATE TABLE "watch_table" (
                         \t"path"\tTEXT
                        );
                        """);
            }
        } catch (SQLException theE) {
            System.err.println("SQL Error: " + theE.getMessage());
            throw new DatabaseException("Error initializing database", theE);
        }
    }

}
