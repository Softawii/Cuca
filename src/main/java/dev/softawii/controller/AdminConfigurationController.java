package dev.softawii.controller;

import com.softawii.curupira.annotations.IArgument;
import com.softawii.curupira.annotations.ICommand;
import com.softawii.curupira.annotations.IGroup;
import dev.softawii.service.DynamicConfigService;
import dev.softawii.util.EmbedUtil;
import io.micronaut.context.annotation.Context;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

@IGroup(name = "admin", description = "Admin configuration", hidden = false)
@Context
public class AdminConfigurationController {

    private static DynamicConfigService service;

    public AdminConfigurationController(DynamicConfigService service) {
        AdminConfigurationController.service = service;
    }

    @ICommand(name = "info", description = "Get the current configuration", permissions = {Permission.ADMINISTRATOR})
    public static void info(SlashCommandInteractionEvent event) {
        event.replyEmbeds(service.getInfo(EmbedUtil.EmbedLevel.INFO)).queue();
    }

    private static TextChannel getTextChannelFromOptional(OptionMapping mapping, String name) {
        if(mapping == null) return null;
        if (mapping.getType() != OptionType.CHANNEL) throw new IllegalArgumentException(name + " must be a channel");
        GuildChannelUnion channel = mapping.getAsChannel();
        if(channel.getType() != ChannelType.TEXT) throw new IllegalArgumentException(name + " must be a text channel");
        return channel.asTextChannel();
    }

    private static Role getRoleFromOptional(OptionMapping mapping, String name) {
        if(mapping == null) return null;
        if (mapping.getType() != OptionType.ROLE) throw new IllegalArgumentException(name + " must be a role");
        return mapping.getAsRole();
    }



    @ICommand(name = "set", description = "Set a configuration", permissions = {Permission.ADMINISTRATOR})
    @IArgument(name = "events-channel", description = "The channel to send admin events to", required = false, type = OptionType.CHANNEL)
    @IArgument(name = "join-server-role", description = "The role to give to new members", required = false, type = OptionType.ROLE)
    @IArgument(name = "verified-role", description = "The role to give to verified members", required = false, type = OptionType.ROLE)
    public static void set(SlashCommandInteractionEvent event) {
        try {
            TextChannel eventsChannel = getTextChannelFromOptional(event.getOption("events-channel"), "events-channel");
            Role joinServerRole = getRoleFromOptional(event.getOption("join-server-role"), "join-server-role");
            Role verifiedRole = getRoleFromOptional(event.getOption("verified-role"), "verified-role");

            service.updateConfig(eventsChannel, joinServerRole, verifiedRole);
            event.replyEmbeds(service.getInfo(EmbedUtil.EmbedLevel.SUCCESS)).queue();
        } catch (IllegalArgumentException e) {
            event.reply(e.getMessage()).addEmbeds(service.getInfo(EmbedUtil.EmbedLevel.ERROR)).setEphemeral(true).queue();
        }
    }
}
