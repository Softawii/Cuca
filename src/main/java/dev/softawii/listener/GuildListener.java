package dev.softawii.listener;

import dev.softawii.service.StudentService;
import io.micronaut.validation.validator.constraints.EmailValidator;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Singleton
public class GuildListener extends ListenerAdapter {
    private final StudentService studentService;
    private final EmailValidator emailValidator = new EmailValidator();

    public GuildListener(JDA jda, StudentService studentService) {
        this.studentService = studentService;
        jda.addEventListener(this);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        User user      = event.getUser();
        Long discordId = user.getIdLong();
        if (studentService.alreadySetup(discordId)) {
            // give role
        } else {
            // send private message
            if (!user.hasPrivateChannel()) {
                // log
                return;
            }

            user.openPrivateChannel()
                    .queue(channel -> {
                        channel.sendMessage("Você acabou de entrar servidor do DCC e não possui cadastro.").queue();
                    });
        }
    }
}
