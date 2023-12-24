package dev.softawii.controller;

import com.softawii.curupira.annotations.IArgument;
import com.softawii.curupira.annotations.IButton;
import com.softawii.curupira.annotations.ICommand;
import com.softawii.curupira.annotations.IGroup;
import dev.softawii.entity.Student;
import dev.softawii.service.DynamicConfigService;
import dev.softawii.service.EventService;
import dev.softawii.service.StudentService;
import dev.softawii.util.EmailUtil;
import dev.softawii.util.EmbedUtil;
import io.micronaut.context.annotation.Context;
import io.micronaut.data.model.Page;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@IGroup(name = "admin", description = "Admin configuration", hidden = false)
@Context
public class AdminConfigurationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminConfigurationController.class);
    private static final int PAGE_SIZE = 5;

    private static DynamicConfigService config;
    private static StudentService studentService;
    private static EventService eventService;

    private static EmbedUtil embedUtil;
    private static EmailUtil emailUtil;

    public AdminConfigurationController(
            DynamicConfigService config,
            StudentService studentService,
            EventService eventService,
            EmbedUtil embedUtil,
            EmailUtil emailUtil
    ) {
        AdminConfigurationController.config = config;
        AdminConfigurationController.studentService = studentService;
        AdminConfigurationController.eventService = eventService;
        AdminConfigurationController.embedUtil = embedUtil;
        AdminConfigurationController.emailUtil = emailUtil;
    }

    @ICommand(name = "info", description = "Get the current configuration", permissions = {Permission.ADMINISTRATOR})
    public static void info(SlashCommandInteractionEvent event) {
        event.replyEmbeds(config.getInfo(EmbedUtil.EmbedLevel.INFO)).queue();
    }

    private static TextChannel getTextChannelFromOptional(OptionMapping mapping, String name) {
        if (mapping == null) return null;
        if (mapping.getType() != OptionType.CHANNEL) throw new IllegalArgumentException(name + " must be a channel");
        GuildChannelUnion channel = mapping.getAsChannel();
        if (channel.getType() != ChannelType.TEXT) throw new IllegalArgumentException(name + " must be a text channel");
        return channel.asTextChannel();
    }

    private static Role getRoleFromOptional(OptionMapping mapping, String name) {
        if (mapping == null) return null;
        if (mapping.getType() != OptionType.ROLE) throw new IllegalArgumentException(name + " must be a role");
        return mapping.getAsRole();
    }

    private static Member getMemberFromOptional(OptionMapping mapping, String name) {
        if (mapping == null) return null;
        if (mapping.getType() != OptionType.USER) throw new IllegalArgumentException(name + " must be a user");
        return mapping.getAsMember();
    }

    private static String getStringFromOptional(OptionMapping mapping, String name) {
        if (mapping == null) return null;
        if (mapping.getType() != OptionType.STRING) throw new IllegalArgumentException(name + " must be a string");
        return mapping.getAsString();
    }

    private static int getIntegerFromOptional(OptionMapping mapping, String name) {
        if (mapping == null) return 0;
        if (mapping.getType() != OptionType.INTEGER) throw new IllegalArgumentException(name + " must be a integer");
        return mapping.getAsInt();
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

            config.updateConfig(eventsChannel, joinServerRole, verifiedRole);
            event.replyEmbeds(config.getInfo(EmbedUtil.EmbedLevel.SUCCESS)).queue();
        } catch (IllegalArgumentException e) {
            event.reply(e.getMessage()).addEmbeds(config.getInfo(EmbedUtil.EmbedLevel.ERROR)).setEphemeral(true).queue();
        }
    }

    @ICommand(name = "verify", description = "Verify a member manually", permissions = {Permission.ADMINISTRATOR})
    @IArgument(name = "member", description = "The member to manually verify", required = true, type = OptionType.USER)
    @IArgument(name = "email", description = "The email of the member", required = true, type = OptionType.STRING)
    public static void verify(SlashCommandInteractionEvent event) {
        Member member = event.getOption("member").getAsMember();
        String email = emailUtil.processEmail(event.getOption("email").getAsString());
        if (!emailUtil.isValidEmail(email)) {
            event.reply("Invalid email").setEphemeral(true).queue();
            return;
        }

        try {
            manuallyVerifyUser(member, email, event.getGuild(), event.getUser());
            event.reply("User verified").setEphemeral(true).queue();
        } catch (Exception e) {
            event.reply("Failed to manually verify user").setEphemeral(true).queue();
        }
    }

    private static void manuallyVerifyUser(Member member, String email, Guild guild, User author) {
        Role joinServerRole = config.getJoinServerRole();
        Role verifiedRole = config.getVerifiedRole();

        if (verifiedRole != null) guild.addRoleToMember(member, verifiedRole).queue();
        if (joinServerRole != null) guild.removeRoleFromMember(member, joinServerRole).queue();

        studentService.createStudent(member.getIdLong(), email);
        LOGGER.info("Member manually verified: " + member.getAsMention());
        eventService.dispatch(embedUtil.generate(
                EmbedUtil.EmbedLevel.SUCCESS,
                "User manually verified",
                String.format("%s manually verified %s", author.getAsMention(), member.getAsMention()),
                null,
                null
        ));
    }

    @ICommand(name = "search", description = "Search for a member info", permissions = {Permission.ADMINISTRATOR})
    @IArgument(name = "member", description = "The member to manually verify", required = false, type = OptionType.USER)
    @IArgument(name = "email", description = "The email of the member", required = false, type = OptionType.STRING)
    public static void search(SlashCommandInteractionEvent event) {
        Member member = getMemberFromOptional(event.getOption("member"), "member");
        String email = getStringFromOptional(event.getOption("email"), "email");

        if((member == null) && (email == null)) {
            event.reply("You must provide a member or an email, not both").setEphemeral(true).queue();
            return;
        }

        // Searching by Member
        Optional<Student> studentOptional = Optional.empty();
        if(member != null) {
            studentOptional = studentService.findByDiscordId(member.getIdLong());
        }
        // Searching by Email
        else {
            studentOptional = studentService.findByEmail(email);
        }

        if(studentOptional.isEmpty()) {
            event.reply("No student found").setEphemeral(true).queue();
            return;
        }

        Student student = studentOptional.get();
        if(member == null) {
            member = event.getGuild().getMemberById(student.getDiscordUserId());
        }

        if(member.getUser() != null) {
            MessageEmbed embed = studentService.getStudentInfo(member.getUser(), student);
            event.replyEmbeds(embed).setEphemeral(true).queue();
        } else {
            event.reply("No user found").setEphemeral(true).queue();
        }
    }

    @ICommand(name = "list", description = "List all students", permissions = {Permission.ADMINISTRATOR})
    @IArgument(name = "page", description = "The page to list", required = false, type = OptionType.INTEGER)
    public static void list(SlashCommandInteractionEvent event) {
        int pageNumber = getIntegerFromOptional(event.getOption("page"), "page");
        Page<Student> page = studentService.findAll(pageNumber, PAGE_SIZE);
        event.replyEmbeds(studentService.getStudentListEmbed(page))
                .addActionRow(studentService.getActionRow(page))
                .setEphemeral(true)
                .queue();

    }

    @IButton(id="list-page")
    public static void listButton(ButtonInteractionEvent event) {
        int pageNumber = Integer.parseInt(event.getComponentId().split(":")[1]);
        Page<Student> page = studentService.findAll(pageNumber, PAGE_SIZE);
        event.editMessageEmbeds(studentService.getStudentListEmbed(page))
                .setActionRow(studentService.getActionRow(page))
                .queue();
    }
}