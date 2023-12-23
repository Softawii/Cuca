package dev.softawii.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    private final JDA    jda;
    private final String eventChannelId;

    public EventService(
            JDA jda,
            @Value("${app.discord.event_channel}") @Nullable String eventChannelId
    ) {
        this.jda = jda;
        this.eventChannelId = eventChannelId;
    }

    public void dispatch(String message) {
        if (this.eventChannelId == null) {
            LOGGER.error("EventService is not properly set up. Message not dispatched: " + message);
            return;
        }

        TextChannel channel = jda.getTextChannelById(this.eventChannelId);
        if (channel == null) {
            LOGGER.error("Failed to dispatch event message: " + message);
            return;
        }
        LOGGER.error("Queued event message: " + message);
        channel.sendMessage(message).queue();
    }
}
