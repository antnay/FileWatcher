package controller;

import model.DBManager;


public class FileWatcherController {

    public static void main(String[] theArgs) {
        DBManager.getDBManager().connect();
        DBManager.getDBManager().addToTable("test 1");
    }
}
