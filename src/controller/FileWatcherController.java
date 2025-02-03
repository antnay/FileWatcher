package controller;

import model.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

public class FileWatcherController {

    public static void main(String[] theArgs) {
        try (Connection connect = DBManager.connect()) {
        } catch (SQLException theE) {
            // FIXME
            System.err.println(theE.getMessage());
            System.exit(1);
        }
    }
}
