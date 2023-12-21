package dev.softawii.config;

import com.softawii.curupira.core.Curupira;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Factory
public class AppConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Context
    public JDA jda(@Value("${discord_token}") String token) {
        JDA jda = JDABuilder.createDefault(token)
                .build();

        if (jda.getGuilds().size() > 1) {
            LOGGER.error("Bot is in more than 1 guild. Shutting down");
            throw new RuntimeException("Bot is in more than 1 guild. Shutting down");
        }
        return jda;
    }

    @Context
    public Curupira curupira(JDA jda, @Value("${curupira.reset:false}") boolean reset) {
        return new Curupira(jda, reset, null, "dev.softawii.controller");
    }
}
