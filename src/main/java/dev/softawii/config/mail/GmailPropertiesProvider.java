package dev.softawii.config.mail;

import io.micronaut.core.annotation.Order;
import io.micronaut.email.javamail.sender.JavaMailConfigurationProperties;
import io.micronaut.email.javamail.sender.MailPropertiesProvider;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.Properties;

@Singleton
@Named("GmailPropertiesProvider")
public class GmailPropertiesProvider implements MailPropertiesProvider {

    private final JavaMailConfigurationProperties configurationProperties;

    public GmailPropertiesProvider(JavaMailConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    @Override
    public Properties mailProperties() {
        Properties properties = new Properties();

        // Customize your mail properties here
        properties.setProperty("mail.smtp.host", configurationProperties.getProperties().get("host").toString());
        properties.setProperty("mail.smtp.port", configurationProperties.getProperties().get("port").toString());
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");

        // You can use values from configurationProperties if needed
        properties.setProperty("mail.smtp.username", configurationProperties.getProperties().get("username").toString());
        properties.setProperty("mail.smtp.password", configurationProperties.getProperties().get("password").toString());

        return properties;
    }
}
