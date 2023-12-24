package dev.softawii.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);
    private final JDA    jda;
    private final DynamicConfigService config;

    public EventService(
            JDA jda,
            DynamicConfigService config
    ) {
        this.jda = jda;
        this.config = config;
    }

    public void dispatch(MessageEmbed embed) {
        if (this.config.getEventChannel() == null) {
            LOGGER.error("EventService is not properly set up. Message not dispatched: " + embed.toData());
            return;
        }

        TextChannel channel = this.config.getEventChannel();
        if (channel == null) {
            LOGGER.error("Failed to dispatch event message: " + embed.toData());
            return;
        }
        LOGGER.info("Queued event message: " + embed.toData());
        channel.sendMessageEmbeds(embed).queue();
    }
}
