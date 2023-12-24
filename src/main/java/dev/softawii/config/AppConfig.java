package dev.softawii.config;

import com.softawii.curupira.core.Curupira;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Factory
public class AppConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Context
    public JDA jda(@Value("${discord_token}") String token, @Value("${dev_env:false}") boolean devEnv) {
        JDA jda;
        try {
            JDABuilder builder = JDABuilder.create(token, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES);
            builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            builder.enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.ROLE_TAGS, CacheFlag.ACTIVITY);
            jda = builder.build();
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!devEnv && jda.getGuilds().size() > 1) {
            LOGGER.error("Bot is in more than 1 guild. Shutting down");
            throw new RuntimeException("Bot is in more than 1 guild. Shutting down");
        }

        return jda;
    }

    @Context
    public Curupira curupira(JDA jda, @Value("${curupira.reset:false}") boolean reset) {
        LOGGER.info("Curupira reset: " + reset);
        return new Curupira(jda, reset, null, "dev.softawii.controller");
    }
}
