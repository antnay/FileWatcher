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
        // TODO: Maybe call connect() right away?
    }

    static DBManager getDBManager() {
        return DB_INSTANCE;
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
            System.err.println("Error connecting to database " + theE.getMessage());
            // TODO: Handle this error
            //theE.printStackTrace(); (If we want to log what the exact error is)
        }
    }

    void disconnect() {
        try {
            if (!isConnected()) {
                connection.close();
                connection = null;
            }
            System.out.println("Disconnected from database");
        } catch (SQLException theE) {
            System.err.println("Error trying to close database.");
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
            // System.err.println("Error clearing table: " + theE.getMessage());
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
            // System.err.println("Error adding event: " + e.getMessage());
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

    /**
     * Checks connection to database.
     *
     * @return true if there is a connection to the database, false otherwise.
     */
    private boolean isConnected() {
        return connection != null;
    }

}
