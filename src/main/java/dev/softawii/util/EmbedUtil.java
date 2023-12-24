package dev.softawii.util;

import jakarta.inject.Singleton;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.time.Instant;

@Singleton
public class EmbedUtil {

    public enum EmbedLevel {
        SUCCESS(new Color(0x80, 0xB9, 0x18)),
        ERROR(new Color(0xFF, 0x35, 0x3F)),
        INFO(new Color(0xF4, 0xF1, 0xDE)),
        WARNING(new Color(0xFF, 0x9F, 0x1C));

        public final Color color;
        EmbedLevel(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return this.color;
        }
    }

    public record Author(String name, String url) {}

    private final JDA jda;

    public EmbedUtil(JDA jda) {
        this.jda = jda;
    }

    public MessageEmbed generate(
            EmbedLevel level,
            String title,
            String description,
            String footer,
            Author author,
            MessageEmbed.Field ... fields

    ) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(title);
        builder.setDescription(description);
        builder.setColor(level.getColor());
        builder.setTimestamp(Instant.now());

        // Author
        if (author != null) {
            builder.setAuthor(author.name(), null, author.url());
        } else {
            builder.setAuthor(this.jda.getSelfUser().getName(), null, this.jda.getSelfUser().getAvatarUrl());
        }

        // Footer
        if (footer != null) builder.setFooter(footer);

        for (MessageEmbed.Field field : fields) {
            builder.addField(field);
        }

        return builder.build();
    }

    public MessageEmbed generate(
            EmbedLevel level,
            String title,
            String description,
            User user
    ) {
        return this.generate(level, title, description, null, new Author(user.getName(), user.getAvatarUrl()));
    }
}
