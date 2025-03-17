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
import org.apache.commons.codec.binary.Base64;

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
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.Set;

import static javax.mail.Message.RecipientType.TO;

class Email {

    private Gmail service;

    /**
     * Initializes Gmail API service.
     */
    public Email() {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            service = new Gmail.Builder(httpTransport, jsonFactory, getCredentials(httpTransport, jsonFactory))
                    .setApplicationName("File Watcher Email")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("Error initializing Gmail API: " + e.getMessage());
            service = null;
        }
    }

    /**
     * Gets authorized Gmail credentials.
     */
    private static Credential getCredentials(final NetHttpTransport httpTransport, GsonFactory jsonFactory) {
        try {
            String credentialsPath = Dotenv.load().get("GOOGLE_APPLICATION_CREDENTIALS", "config/credentials.json");

            File credentialsFile = new File(credentialsPath);
            if (!credentialsFile.exists()) {
                throw new FileNotFoundException("Error: Credentials file not found at " + credentialsPath);
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory,
                    new FileReader(credentialsFile));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, jsonFactory, clientSecrets, Set.of(GmailScopes.GMAIL_COMPOSE))
                    .setDataStoreFactory(new FileDataStoreFactory(Paths.get("token").toFile()))
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (IOException e) {
            System.err.println("Error loading credentials: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the authenticated user's email from Gmail API.
     */
    private String getAuthenticatedUserEmail() {
        try {
            return service.users().getProfile("me").execute().getEmailAddress();
        } catch (IOException e) {
            System.err.println("Error fetching authenticated email: " + e.getMessage());
            return "unknown_user@gmail.com";
        }
    }

    /**
     * Creates a Gmail draft with the CSV file attached.
     */
    public void sendEmailWithLogFile(String recipientEmail, File attachment) {
        if (service == null) {
            System.err.println("Error: Gmail service not initialized. Check authentication.");
            return;
        }
        try {
            String senderEmail = getAuthenticatedUserEmail();
            MimeMessage email = createMimeMessage(senderEmail, recipientEmail, attachment);
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
     * Creates a MIME message with an attachment.
     */
    private MimeMessage createMimeMessage(String fromEmail, String toEmail, File file)
            throws MessagingException, IOException {

        String subject = "Your File Watcher Log";
        String bodyText = "Find the attached log file.";

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmail));
        email.addRecipient(TO, new InternetAddress(toEmail));
        email.setSubject(subject);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(bodyText, "utf-8");

        MimeBodyPart filePart = new MimeBodyPart();
        if (file.length() == 0) {
            System.out.println("Empty File.");
        }
        DataSource source = new FileDataSource(file);
        filePart.setDataHandler(new DataHandler(source));
        filePart.setFileName(file.getName());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(filePart);
        email.setContent(multipart);

        return email;
    }

    /**
     * Creates a Gmail draft message.
     */
    private Message createDraftMessage(MimeMessage email) throws IOException, MessagingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
        Message message = new Message();
        message.setRaw(encodedEmail);

        Draft draft = new Draft();
        draft.setMessage(message);

        try {
            draft = service.users().drafts().create("me", draft).execute();
            System.out.println("Draft created with ID: " + draft.getId());
            return draft.getMessage();
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            System.err.println("Error creating draft: " + error.getMessage());
            return null;
        }
    }

    /**
     * Opens Gmail in a browser with the draft loaded.
     */
    private void openGmailWithDraft(String draftId) {
        try {
            String url = "https://mail.google.com/mail/u/0/#drafts?compose=" + draftId;
            Desktop.getDesktop().browse(new URI(url));
            System.out.println("Opened Gmail with draft.");
        } catch (Exception e) {
            System.err.println("Error opening Gmail: " + e.getMessage());
        }
    }
}
