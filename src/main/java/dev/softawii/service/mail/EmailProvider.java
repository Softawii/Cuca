package dev.softawii.service.mail;

import dev.softawii.exceptions.FailedToSendEmailException;
import jakarta.mail.Address;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;

public interface EmailProvider {
    void send(String to, String subject, String content) throws FailedToSendEmailException;

    default Address parseEmail(String email, String personal) {
        try {
            return new InternetAddress(email, personal);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    default Address parseEmail(String email) {
        try {
            return new InternetAddress(email);
        } catch (AddressException e) {
            throw new RuntimeException(e);
        }
    }
}
