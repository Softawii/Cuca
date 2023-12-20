package dev.softawii.listener;

import dev.softawii.service.StudentService;
import io.micronaut.validation.validator.constraints.EmailValidator;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Singleton
public class PrivateListener extends ListenerAdapter {
    private final StudentService studentService;
    private final EmailValidator emailValidator = new EmailValidator();

    public PrivateListener(JDA jda, StudentService studentService) {
        this.studentService = studentService;
        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.PRIVATE)) {
            return;
        }
    }

    enum ChannelState {
        WAITING_EMAIL
    }
}
