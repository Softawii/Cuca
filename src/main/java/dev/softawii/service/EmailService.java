package dev.softawii.service;

import dev.softawii.exceptions.FailedToSendEmailException;
import dev.softawii.service.mail.EmailProvider;
import jakarta.inject.Singleton;

@Singleton
public class EmailService {

    private final EmailProvider emailProvider;

    public EmailService(EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    public void send(String to, String subject, String content) throws FailedToSendEmailException {
        emailProvider.send(to, subject, content);
    }
}
