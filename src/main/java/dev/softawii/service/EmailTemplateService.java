package dev.softawii.service;

import io.micronaut.core.io.scan.ClassPathResourceLoader;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class EmailTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTemplateService.class);

    private final ClassPathResourceLoader resourceLoader;
    private final Map<String, String>     resourceCache;

    public EmailTemplateService(ClassPathResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.resourceCache = HashMap.newHashMap(1);
    }

    private String loadResource(String path) {
        Optional<InputStream> resourceAsStream = resourceLoader.getResourceAsStream(path);

        if (resourceAsStream.isEmpty()) {
            LOGGER.error("Failed to load template resource file: " + path);
            throw new RuntimeException("Failed to load template resource file: " + path);
        }

        try {
            return new String(resourceAsStream.get().readAllBytes());
        } catch (IOException e) {
            LOGGER.error("Failed to load template resource file: " + path, e);
            throw new RuntimeException("Failed to load template resource file: " + path, e);
        }
    }

    public String parseTemplate(String templateName, Map<String, String> model) {
        String template = resourceCache.getOrDefault(templateName, loadResource("email-template/" + templateName + ".html"));

        for (Map.Entry<String, String> entry : model.entrySet()) {
            template = template.replace(entry.getKey(), entry.getValue());
        }

        return template;
    }
}
