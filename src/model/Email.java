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
      * //      * Create a draft email with attachment.
      * //      *
      * //      * @param fromEmailAddress - Email address to appear in the from: header.
      * //      * @param toEmailAddress   - Email address of the recipient.
      * //      * @param file             - Path to the file to be attached.
      * //      * @return the created draft, {@code null} otherwise.
      * //      * @throws MessagingException - if a wrongly formatted address is encountered.
      * //      * @throws IOException        - if service account credentials file not found.
      * //
      */

     static Draft createDraftMessageWithAttachment(String fromEmailAddress,
                                                   String toEmailAddress,
                                                   File file)
             throws MessagingException, IOException {
         /* Load pre-authorized user credentials from the environment.
          TODO(developer) - See https://developers.google.com/identity for
           guides on implementing OAuth2 for your application.*/
         GoogleCredentials credentials;
         try {
             credentials = GoogleCredentials.getApplicationDefault()
                     .createScoped(GmailScopes.GMAIL_COMPOSE);
         } catch (IOException e) {
             throw new IOException("Google credentials could not be loaded.", e);
         }

         //GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                 //.createScoped(GmailScopes.GMAIL_COMPOSE);

         HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

         // Create the gmail API client
         Gmail service = new Gmail.Builder(new NetHttpTransport(),
                 GsonFactory.getDefaultInstance(),
                 requestInitializer)
                 .setApplicationName("Gmail samples")
                 .build();

         //This will create the email message
         Message message = createMimeMessage (fromEmailAddress, toEmailAddress, file);

         try {
             // Create the draft message
             Draft draft = new Draft();
             draft.setMessage(message);
             draft = service.users().drafts().create("me", draft).execute();
             System.out.println("Draft id: " + draft.getId());
             System.out.println(draft.toPrettyString());
             return draft;
         } catch (GoogleJsonResponseException e) {
             GoogleJsonError error = e.getDetails();
             if (error.getCode() == 403) {
                 System.err.println("Unable to create draft: " + e.getDetails());
             } else {
                 throw e;
             }
         }
         return null;
     }

     /**
      * Creates the actual email message content.
      *
      * @param fromEmailAddress The sender's email address.
      * @param toEmailAddress The recipient's email address.
      * @param file The file to attach.
      * @return
      * @throws MessagingException If there are issues with the email format.
      * @throws IOException If file handling fails.
      */

     private static Message createMimeMessage(String fromEmailAddress, String toEmailAddress, File file)
             throws MessagingException, IOException {

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

         return message;

     }

     /**
      * Generates a CSV file from the database records.
      *
      * @return The generated CSV file.
      */

     static File generateCSV() {

         File logFile = new File("database/file_watcher_log.csv");

         try {
             BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
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
             System.err.println("Error writing CSV: " + theE.getMessage());
         }
         return logFile;
     }

     /**
      * Sends an email with a CSV log file attached.
      *
      * @param recipientEmail The recipient's email address.
      */

     void sendEmailWithLogFile(String recipientEmail) {
         try {

             // Load the sender’s email address from environment variables (retrieves and stores sender's email address)
             GoogleCredentials credentials;
             try {
                 credentials = GoogleCredentials.getApplicationDefault()
                         .createScoped(GmailScopes.GMAIL_COMPOSE);
             } catch (IOException e) {
                 throw new IOException("Could not load Google credentials.", e);
             }

             HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

             // Create Gmail service (connects to Gmail’s API)
             Gmail service = new Gmail.Builder(new NetHttpTransport(),
                     GsonFactory.getDefaultInstance(),
                     requestInitializer)
                     .setApplicationName("Gmail samples")
                     .build();

             // Generate CSV file
             File logFile = generateCSV();

             // Create and send the email with attachment
             Draft draft = createDraftMessageWithAttachment("me", recipientEmail, logFile);

             if (draft != null) {
                 service.users().messages().send("me", draft.getMessage()).execute();
                 System.out.println("Email sent successfully.");
             } else {
                 System.err.println("Failed to create the draft.");
             }

         } catch (MessagingException | IOException e) {
             throw new RuntimeException("Error sending email", e);
         }
     }

      /*void sendEmail(Draft theDraft) {
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

     } */
 }