package dev.softawii.service.mail;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Singleton
@Named("gmail")
public class GmailService implements EmailProvider {

    private static final Logger    LOGGER = LoggerFactory.getLogger(GmailService.class);
    private              Transport transport;
    private              Session   session;

    private final String from;
    private final String username;
    private final String password;

    public GmailService(
            @Value("${mail.from}") String from,
            @Value("${mail.username}") String username,
            @Value("${mail.password}") String password
    ) {
        this.from = from;
        this.username = username;
        this.password = password;
        setupSession();
    }

    @Override
    public void send(String to, String subject, String content) {
        try {
            send(to, subject, content, 1);
        } catch (MessagingException e) {
            setupSession();
            e.printStackTrace();
        }
    }

    public void send(String to, String subject, String content, int tentative) throws MessagingException {
        if (tentative > 2) {
            throw new RuntimeException();
        }
        Message msg;
        try {
            msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from, "CUCA - DCC BOT"));
            msg.setSubject(subject);
            msg.setText(content);
            transport.sendMessage(msg, new InternetAddress[]{new InternetAddress(to)});
        } catch (AddressException | UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void setupSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Authenticator authenticator = new GmailAuthenticator(this.username, this.password);
        this.session = Session.getDefaultInstance(properties, authenticator);
        try {
            this.transport = this.session.getTransport("smtp");
            this.transport.connect(this.username, this.password);
        } catch (Exception e) {
            throw new RuntimeException();
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
