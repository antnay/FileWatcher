package model;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import io.github.cdimascio.dotenv.Dotenv;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Properties;

class Email {
    // from google api docs https://developers.google.com/gmail/api/guides/drafts
    /**
     * Create a draft email with attachment.
     *
     * @param fromEmailAddress - Email address to appear in the from: header.
     * @param toEmailAddress   - Email address of the recipient.
     * @param file             - Path to the file to be attached.
     * @return the created draft, {@code null} otherwise.
     * @throws MessagingException - if a wrongly formatted address is encountered.
     * @throws IOException        - if service account credentials file not found.
     */
    static Draft createDraftMessageWithAttachment(String fromEmailAddress,
                                                  String toEmailAddress,
                                                  File file)
            throws MessagingException, IOException {
        /* Load pre-authorized user credentials from the environment.
         TODO(developer) - See https://developers.google.com/identity for
          guides on implementing OAuth2 for your application.*/
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(GmailScopes.GMAIL_COMPOSE);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        // Create the gmail API client
        Gmail service = new Gmail.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Gmail samples")
                .build();

        // Create the email content
        String messageSubject = "Test message";
        String bodyText = "lorem ipsum.";

        // Encode as MIME message
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmailAddress));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(toEmailAddress));
        email.setSubject(messageSubject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(bodyText, "text/plain");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        mimeBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(file);
        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName(file.getName());
        multipart.addBodyPart(mimeBodyPart);
        email.setContent(multipart);

        // Encode and wrap the MIME message into a gmail message
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
        Message message = new Message();
        message.setRaw(encodedEmail);

        try {
            // Create the draft message
            Draft draft = new Draft();
            draft.setMessage(message);
            draft = service.users().drafts().create("me", draft).execute();
            System.out.println("Draft id: " + draft.getId());
            System.out.println(draft.toPrettyString());
            return draft;
        } catch (GoogleJsonResponseException e) {
            // TODO(developer) - handle error appropriately
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == 403) {
                System.err.println("Unable to create draft: " + e.getDetails());
            } else {
                throw e;
            }
        }
        return null;
    }

    static void fillCSV(File theCSV) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(theCSV));
            ResultSet resSet = DBManager.getDBManager().getTable();
            ResultSetMetaData metaData = resSet.getMetaData();
            int colNum = metaData.getColumnCount();
            for (int i = 1; i < colNum; i++) {
                writer.append('"').append(metaData.getColumnName(i)).write("\",");
            }
            writer.append('"').append(metaData.getColumnName(colNum)).append("\"\n");
            while (resSet.next()) {
                for (int i = 1; i < colNum; i++) {
                    writer.append('"').append(resSet.getString(i)).write("\",");
                }
                writer.append('"').append(resSet.getString(colNum)).write("\"\n");
            }
            writer.close();
        } catch (DatabaseException | SQLException | IOException theE) {
            // TODO: DO ME
        }
    }

    void sendEmail(Draft theDraft) {
        Dotenv dotenv = Dotenv.load();
        File file = null;
        file = new File("database/log.csv");
        fillCSV(file);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
        Gmail service = new Gmail.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Gmail samples")
                .build();
        try {
            Draft draft = createDraftMessageWithAttachment(dotenv.get("EMAIL"), dotenv.get("EMAIL"), file);

        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }

    }
}
