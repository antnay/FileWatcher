package model;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

class EmailTest {
    @BeforeAll
    static void beforeAll() {
        try {
            DBManager.getDBManager().connect();
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.jupiter.api.Test
    void createDraftMessageWithAttachment() {
        Dotenv dotenv = Dotenv.load();
        System.out.println(dotenv.get("EMAIL"));

//        Email.createDraftMessageWithAttachment(dotenv.get("EMAIL"), dotenv.get("EMAIL"));
    }

    @Test
    void createCSV() {
        File tFile = Email.fillCSV();
        System.out.println(tFile.getAbsolutePath());
    }
}
