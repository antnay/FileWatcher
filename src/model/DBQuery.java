package model;

import java.sql.Connection;

public class DBQuery {

    public DBQuery() {
    }

    public void query(String theQuery) {
        DBManager.getDBManager().executeQuery(theQuery);
    }

}
