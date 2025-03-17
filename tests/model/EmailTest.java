package model;

/* import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

class EmailTest {
    @BeforeAll
    // It establishes a database connection before it runs any tests
    static void beforeAll() {
        try {
            DBManager.getDBManager().connect();
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createDraftMessageWithAttachment() {
        // This gets the sender and recipient emails from .env file
        Dotenv dotenv = Dotenv.load();
        String senderEmail = dotenv.get("EMAIL");
        String recipientEmail = dotenv.get("EMAIL");
        System.out.println(dotenv.get("EMAIL"));

        // This creates a CSV file with the database data
//        File logFile = Email.generateCSV();

        // Email.createDraftMessageWithAttachment(dotenv.get("EMAIL"),
        // dotenv.get("EMAIL"));

        // Tell us whether a draft email with the CSV file attached was created
        // successfully
        try {
            System.out.println("Draft created: "
                    + Email.createDraftMessageWithAttachment(senderEmail, recipientEmail, logFile).getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void generateCSV() {
        File tFile = Email.generateCSV();
        System.out.println(tFile.getAbsolutePath());
    }

    @Test
    // Tests whether an email with the CSV attachment was sent successfully
    void sendEmailWithLogFile() {
        Dotenv dotenv = Dotenv.load();
        String recipientEmail = dotenv.get("EMAIL");

        Email email = new Email();
        email.sendEmailWithLogFile(recipientEmail);

        System.out.println("Email send attempt completed.");
    }
} */
