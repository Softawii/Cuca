package dev.softawii.service;

import io.micronaut.email.Email;
import io.micronaut.email.EmailSender;
import jakarta.inject.Singleton;

@Singleton
public class EmailService {
    private final EmailSender<?, ?> emailSender;

    public EmailService(EmailSender<?, ?> emailSender) {
        this.emailSender = emailSender;
    }

    public void send(String email, String token) {
        emailSender.send(Email.builder()
                .from("sender@example.com")
                .to("john@example.com")
                .subject("Cuca - Authentication Token")
                .body(token));
    }
}
