package dev.softawii.service.mail;

import dev.softawii.exceptions.FailedToSendEmailException;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.mail.*;
import jakarta.mail.event.ConnectionEvent;
import jakarta.mail.event.ConnectionListener;
import jakarta.mail.event.TransportEvent;
import jakarta.mail.event.TransportListener;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

@Singleton
@Named("gmail")
public class GmailService implements EmailProvider {

    private static final Logger    LOGGER = LoggerFactory.getLogger(GmailService.class);
    private              Transport transport;
    private              Session session;
    private final Properties     defaultProperties;

    private final String from;
    private final String username;
    private final String password;

    public GmailService(
            @Value("${mail.from}") String from,
            @Value("${mail.username}") String username,
            @Value("${mail.password}") String password
    ) {
        this.defaultProperties = new Properties();
        defaultProperties.put("mail.smtp.host", "smtp.gmail.com");
        defaultProperties.put("mail.smtp.port", "587");
        defaultProperties.put("mail.smtp.auth", "true");
        defaultProperties.put("mail.smtp.starttls.enable", "true");

        this.from = from;
        this.username = username;
        this.password = password;
    }

    @Override
    public void send(String to, String subject, String content) throws FailedToSendEmailException {
        send(to, subject, content, 1);
    }

    public void send(String to, String subject, String content, int tentative) throws FailedToSendEmailException {
        setupSession();
        if (tentative > 2) {
            LOGGER.error("Failed to send email");
            throw new FailedToSendEmailException();
        }

        Message msg = new MimeMessage(session);
        try {
            msg.setSubject(subject);
            msg.setText(content);
            msg.setFrom(parseEmail(from, "CUCA - DCC BOT"));
        } catch (MessagingException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        try {
            transport.sendMessage(msg, new Address[]{parseEmail(to)});
        } catch (AddressException e) {
            LOGGER.error("Unable to parse email: %s".formatted(to), e);
            throw new RuntimeException(e);
        } catch (SendFailedException e) {
            LOGGER.error("Invalid email: " + Arrays.toString(e.getInvalidAddresses()), e);
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            LOGGER.error("Connection failed when sending email. Trying again");
            send(to, subject, content, tentative + 1);
        }
    }

    private boolean isConnectionValid() {
        if (session == null || transport == null) {
            return false;
        }

        if (!transport.isConnected()) {
            return false;
        }

        return true;
    }

    private void setupSession() {
        if (isConnectionValid()) {
            return;
        }
        LOGGER.info("Session is invalid. Setting up");

        Authenticator authenticator = new GmailAuthenticator(this.username, this.password);
        this.session = Session.getDefaultInstance(defaultProperties, authenticator);
        try {
            transport = this.session.getTransport("smtp");
            transport.addTransportListener(new GmailTransportListener());
            transport.addConnectionListener(new GmailConnectionListener());
            transport.connect(this.username, this.password);
            LOGGER.info("Session set up");
        } catch (Exception e) {
            LOGGER.info("Failed to setup session");
            throw new RuntimeException();
        }
    }

    private static class GmailConnectionListener implements ConnectionListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(GmailConnectionListener.class);

        @Override
        public void opened(ConnectionEvent e) {
            LOGGER.info("Connection opened");
        }

        @Override
        public void disconnected(ConnectionEvent e) {
            LOGGER.info("Connection disconnected");
        }

        @Override
        public void closed(ConnectionEvent e) {
            LOGGER.info("Connection closed");
        }
    }

    private static class GmailTransportListener implements TransportListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(GmailTransportListener.class);

        @Override
        public void messageDelivered(TransportEvent e) {
            LOGGER.info("Message delivered - " + parseEvent(e));
        }

        @Override
        public void messageNotDelivered(TransportEvent e) {
            LOGGER.info("Message not delivered - " + parseEvent(e));
        }

        @Override
        public void messagePartiallyDelivered(TransportEvent e) {
            LOGGER.info("Message partially delivered - " + parseEvent(e));
        }

        private Map<String, String> parseEvent(TransportEvent e) {
            return Map.of(
                    "Invalid addresses", Arrays.toString(e.getInvalidAddresses()),
                    "Invalid valid sent addresses", Arrays.toString(e.getValidSentAddresses()),
                    "Invalid valid unsent addresses", Arrays.toString(e.getValidUnsentAddresses())
            );
        }
    }

    private static class GmailAuthenticator extends Authenticator {
        private final String username;
        private final String password;

        private GmailAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
