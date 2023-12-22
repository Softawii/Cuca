package dev.softawii.service;

import dev.softawii.exceptions.FailedToSendEmailException;
import dev.softawii.service.mail.EmailProvider;
import jakarta.inject.Singleton;

import java.util.concurrent.BlockingQueue;

@Singleton
public class EmailService extends Thread {

    private final EmailProvider emailProvider;
    public EmailService(EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
        this.start();
    }

    @Override
    public void run() {

    }

    public void send(String to, String subject, String content) throws FailedToSendEmailException {
        emailProvider.send(to, subject, content);
    }
}
