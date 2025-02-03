package model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager {
    public static Connection connect() {
        File dbPath = new File("database/log.db");
        System.out.println(dbPath.getAbsolutePath());
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
        System.out.println(dbUrl);
        try {
            return DriverManager.getConnection(dbUrl);
        } catch (SQLException theE) {
            System.err.println("Error connecting to database " + theE.getMessage());
            System.exit(1);
        }
    }
}
