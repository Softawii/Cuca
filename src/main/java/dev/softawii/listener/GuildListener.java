package dev.softawii.listener;

import dev.softawii.service.DynamicConfigService;
import dev.softawii.service.StudentService;
import io.micronaut.context.annotation.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
public class GuildListener extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuildListener.class);
    private final StudentService studentService;
    private final DynamicConfigService configService;

    public GuildListener(JDA jda, StudentService studentService, DynamicConfigService configService) {
        this.studentService = studentService;
        this.configService = configService;
        jda.addEventListener(this);
        LOGGER.info("GuildListener registered");
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        Role joinServerRole = configService.getJoinServerRole();
        Role verifiedRole = configService.getVerifiedRole();

        if(joinServerRole == null || verifiedRole == null) {
            LOGGER.error("Server roles not set up");
            return;
        }

        if(member.getGuild() != joinServerRole.getGuild()) {
            LOGGER.error("Member joined a guild that is not the same as the join server role guild. Member: " + member + " Guild Id: " + member.getGuild().getId());
            return;
        }

        if (studentService.alreadySetup(member.getIdLong())) {
            LOGGER.info("Member already setup: " + member.getAsMention());
            event.getGuild().addRoleToMember(member, verifiedRole).queue();
        } else {
            LOGGER.info("Member joined: " + member.getAsMention());
            event.getGuild().addRoleToMember(member, joinServerRole).queue();
        }
    }
}
