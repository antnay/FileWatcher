package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager {
    public static void connect() {
        Path dbPath = Paths.get("../resources/test_dbs/db.sql");
        String dbUrl = "jdbc:sqlite:" + dbPath;
        if (!Files.exists(dbPath)) {
            try {
                Files.createFile(dbPath);
            } catch (IOException theE) {
                System.err.println("Error creating database at " + dbPath);
                System.exit(1);
            }
        }

        try {
            Connection dbCon = DriverManager.getConnection(dbUrl);
        } catch (SQLException theE) {
            System.err.println("Error connect to database at " + dbPath);
            System.exit(1);
        }

    }
}
