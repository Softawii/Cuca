package dev.softawii.service;

import dev.softawii.exceptions.FailedToSendEmailException;
import dev.softawii.service.mail.EmailProvider;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Singleton
public class EmailService extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final EmailProvider emailProvider;
    private final BlockingQueue<EmailInfo> emailQueue;

    public EmailService(@Named("gmail") EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
        this.emailQueue = new LinkedBlockingQueue<>();
        this.setName("EmailThread-1");
        this.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                EmailInfo emailInfo = emailQueue.take();
                this.send(emailInfo.to(), emailInfo.subject(), emailInfo.content());
            } catch (InterruptedException e) {
                LOGGER.error("Email thread interrupted", e);
                System.exit(1);
            } catch (FailedToSendEmailException e) {
                LOGGER.error("Failed to send email", e);
            }
        }
    }

    private void send(String to, String subject, String content) throws FailedToSendEmailException {
        emailProvider.send(to, subject, content);
    }

    public boolean enqueue(String to, String subject, String content) {
        return emailQueue.add(new EmailInfo(to, content, subject));
    }

    private record EmailInfo(String to, String content, String subject) {}
}
