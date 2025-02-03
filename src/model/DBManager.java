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

public final class DBManager {

    private static final DBManager DB_INSTANCE = new DBManager();

    private Connection connection;

    private DBManager() {
        connection = null;
        // TODO: Maybe call connect() right away?
    }

    public static DBManager getDBManager() {
        return DB_INSTANCE;
    }

    public void connect() {
        File dbPath = new File("database/log.db");
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
            initDB();
        } catch (SQLException theE) {
            System.err.println("Error connecting to database " + theE.getMessage());
            // TODO: Handle this error
        }
    }

    public void disconnect() {
        try {
            if (!isConnected()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException theE) {
            System.err.println("Error trying to close database.");
            // TODO: Handle this error
        }
    }

    // test
    public void addToTable(String event) {
        String insertSQL = "INSERT INTO eventlog(event) VALUES(?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            preparedStatement.setString(1, event);
            preparedStatement.executeUpdate();
            System.out.println("Inserted event: " + event);
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting event", e);
            // TODO: Handle this error
        }
    }

    private void initDB() {
        if (!isConnected()) {
            throw new IllegalStateException("Cannot add to table while disconnected");
        }
        try (Statement statement = connection.createStatement()) {
            ResultSet res = statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='eventlog';"
            );
            if (!res.next()) {
                System.out.println("making new table");
                statement.executeUpdate("CREATE TABLE \"eventlog\" (\n" +
                        "\t\"id\"\tINTEGER NOT NULL UNIQUE,\n" +
                        "\t\"event\"\tTEXT,\n" +
                        "\t\"path\"\tTEXT,\n" +
                        "\t\"Timestamp\"\tDATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                        "\tPRIMARY KEY(\"id\" AUTOINCREMENT)\n" +
                        ");");
                System.out.println("added table");
            }
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
