package model;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

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

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Set;

import static javax.mail.Message.RecipientType.TO;

class Email {

    private Gmail myService;

    /**
     * Initializes Gmail API service and sets up authentication.
     */
    public Email() {
        try {
            NetHttpTransport theHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory theJsonFactory = GsonFactory.getDefaultInstance();
            myService = new Gmail.Builder(theHttpTransport, theJsonFactory, getCredentials(theHttpTransport, theJsonFactory))
                    .setApplicationName("File Watcher Email")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("Error initializing Gmail API: " + e.getMessage());
            myService = null;
        }
    }

    /**
     * It checks if the Google credentials file exists.
     * @return true, if the file exists, otherwise, it returns false.
     */
    static boolean gCheck() {
        String credentialsPath = "";
        try {
            credentialsPath = Dotenv.load().get("GOOGLE_APPLICATION_CREDENTIALS", "config/credentials.json");
        } catch (DotenvException theE) {
            System.err.println("No env found");
            credentialsPath = "config/credentials.json";
        }
        File credentialsFile = new File(credentialsPath);
        if (!credentialsFile.exists()) {
            return false;
        }
        return true;
    }

    /**
     * Gets Gmail API credentials.
     *
     * @param theHttpTransport HTTP transport.
     * @param theJsonFactory JSON factory for API processing.
     * @return object for Gmail authentication.
     */
    private Credential getCredentials(final NetHttpTransport theHttpTransport, GsonFactory theJsonFactory) {
        String credentialsPath = "";
        try {
            credentialsPath = Dotenv.load().get("GOOGLE_APPLICATION_CREDENTIALS", "config/credentials.json");
        } catch (DotenvException theE) {
            System.err.println("No env found");
            credentialsPath = "config/credentials.json";
        }
        try {
            File credentialsFile = new File(credentialsPath);
            if (!credentialsFile.exists()) {
                throw new FileNotFoundException("Error: Credentials file not found at " + credentialsPath);
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(theJsonFactory,
                    new FileReader(credentialsFile));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                     theHttpTransport, theJsonFactory, clientSecrets, Set.of(GmailScopes.GMAIL_COMPOSE))
                    .setDataStoreFactory(new FileDataStoreFactory(Paths.get("token").toFile()))
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (IOException e) {
            System.err.println("Error loading credentials email will NOT function");
            // System.err.println(e.getCause());
            return null;
        }
    }

    /**
     * Gets the authenticated user's email address.
     *
     * @return The user's email or "unknown_user@gmail.com" if an error occurs.
     */
    private String getAuthenticatedUserEmail() {
        try {
            return myService.users().getProfile("me").execute().getEmailAddress();
        } catch (IOException e) {
            System.err.println("Error fetching authenticated email: " + e.getMessage());
            return "unknown_user@gmail.com";
        }
    }

    /**
     * It sends an email with a log file attachment by creating a Gmail draft.
     *
     * @param theRecipientEmail Recipient's email address.
     * @param theAttachment File to attach.
     */
    public void sendEmailWithLogFile(String theRecipientEmail, File theAttachment) {
        if (myService == null) {
            System.err.println("Error: Gmail service not initialized. Check authentication.");
            return;
        }
        try {
            String senderEmail = getAuthenticatedUserEmail();
            MimeMessage email = createMimeMessage(senderEmail, theRecipientEmail, theAttachment);
            Message message = createDraftMessage(email);
            if (message != null) {
                openGmailWithDraft(message.getId());
            } else {
                System.err.println("Failed to create Gmail draft.");
            }
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    /**
     * Creates an email with an attachment.
     *
     * @param theFromEmail Sender's email.
     * @param theToEmail Recipient's email.
     * @param theFile File to attach.
     * @return A MimeMessage object.
     * @throws MessagingException If an error occurs while creating the email.
     * @throws IOException If an error occurs reading the file.
     */
    private MimeMessage createMimeMessage(String theFromEmail, String theToEmail, File theFile)
            throws MessagingException, IOException {

        String subject = "File Watcher Log";
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        // System.out.println("Formatted DateTime: " + formattedDateTime);
        String bodyText = "Log file from " + formattedDateTime;

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(theFromEmail));
        email.addRecipient(TO, new InternetAddress(theToEmail));
        email.setSubject(subject);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(bodyText, "utf-8");

        MimeBodyPart filePart = new MimeBodyPart();
        if (theFile.length() == 0) {
            System.out.println("Empty File.");
        }
        DataSource source = new FileDataSource(theFile);
        filePart.setDataHandler(new DataHandler(source));
        filePart.setFileName(theFile.getName());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(filePart);
        email.setContent(multipart);

        return email;
    }

    /**
     * Creates a Gmail draft message.
     *
     * @param theEmail The email content that will be stored as a draft.
     * @return The created draft message.
     * @throws IOException If an error occurs while saving the draft.
     * @throws MessagingException If an error occurs creating the message.
     */
    private Message createDraftMessage(MimeMessage theEmail) throws IOException, MessagingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        theEmail.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
        Message message = new Message();
        message.setRaw(encodedEmail);

        Draft draft = new Draft();
        draft.setMessage(message);

        try {
            draft = myService.users().drafts().create("me", draft).execute();
            // System.out.println("Draft created with ID: " + draft.getId());
            return draft.getMessage();
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            System.err.println("Error creating draft: " + error.getMessage());
            return null;
        }
    }

    /**
     * Opens Gmail in a browser with the draft loaded.
     *
     * @param theDraftId The unique ID of the draft to open.
     */
    private void openGmailWithDraft(String theDraftId) {
        try {
            String url = "https://mail.google.com/mail/u/0/#drafts?compose=" + theDraftId;
            Desktop.getDesktop().browse(new URI(url));
            // System.out.println("Opened Gmail with draft.");
        } catch (Exception e) {
            System.err.println("Error opening Gmail: " + e.getMessage());
        }
    }
}
