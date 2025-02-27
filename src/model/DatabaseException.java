package model;

public class DatabaseException extends Exception {
    public DatabaseException(String theMessage) {
        super(theMessage);
    }
    public DatabaseException(String theMessage, Throwable theCause) {
        super(theMessage, theCause);
    }
}