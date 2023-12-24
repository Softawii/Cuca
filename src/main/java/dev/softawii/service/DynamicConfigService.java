package dev.softawii.service;

import dev.softawii.entity.DynamicConfig;
import dev.softawii.repository.DynamicConfigurationRepository;
import dev.softawii.util.EmbedUtil;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class DynamicConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigService.class);
    private final DynamicConfigurationRepository repository;
    private final JDA jda;
    private DynamicConfig config;
    private EmbedUtil embedUtil;


    public DynamicConfigService (JDA jda, DynamicConfigurationRepository repository, EmbedUtil embedUtil) {
        this.repository = repository;
        this.jda = jda;
        this.embedUtil = embedUtil;
        setupConfig();
    }

    private void setupConfig() {
        Optional<DynamicConfig> dynamicConfigOptional = repository.find();
        if(dynamicConfigOptional.isEmpty()) {
            DynamicConfig config = new DynamicConfig();
            config.setId(0L);
            config.setEventChannelId(null);
            config.setJoinServerRole(null);
            config.setVerifiedRoleId(null);

            this.config = repository.saveAndFlush(config);
            LOGGER.info("Creating DynamicConfig: " + config);
        } else {
            this.config = dynamicConfigOptional.get();
        }
    }

    public void updateConfig(TextChannel eventChannel, Role joinServerRole, Role verifiedRole) {
        if (eventChannel != null) this.config.setEventChannelId(eventChannel.getIdLong());
        if (joinServerRole != null) this.config.setJoinServerRole(joinServerRole.getIdLong());
        if (verifiedRole != null) this.config.setVerifiedRoleId(verifiedRole.getIdLong());

        LOGGER.info("Updating DynamicConfig: " + this.config);
        repository.update(this.config);
    }

    public MessageEmbed getInfo(EmbedUtil.EmbedLevel level) {
        return embedUtil.generate(
                level,
                "Bot Configuration",
                "Current configuration",
                "Any problems? Contact us in https://github.com/Softawii/cuca",
                null,
                new MessageEmbed.Field("Event Channel", getEventChannel() == null ? "Not set" : getEventChannel().getAsMention(), true),
                new MessageEmbed.Field("Join Server Role", getJoinServerRole() == null ? "Not set" : getJoinServerRole().getAsMention(), true),
                new MessageEmbed.Field("Verified Role", getVerifiedRole() == null ? "Not set" : getVerifiedRole().getAsMention(), true)
        );
    }

    public TextChannel getEventChannel() {
        if (this.config.getEventChannelId() == null) return null;
        return jda.getTextChannelById(this.config.getEventChannelId());
    }

    public Role getJoinServerRole() {
        if (this.config.getJoinServerRole() == null) return null;
        return this.jda.getRoleById(this.config.getJoinServerRole());
    }

    public Role getVerifiedRole() {
        if (this.config.getVerifiedRoleId() == null) return null;
        return this.jda.getRoleById(this.config.getVerifiedRoleId());
    }
}
