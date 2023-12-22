package dev.softawii.service.mail;

public interface EmailProvider {
    void send(String to, String subject, String content);
}
