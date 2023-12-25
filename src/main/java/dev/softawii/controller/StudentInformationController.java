package dev.softawii.controller;

import com.softawii.curupira.annotations.ICommand;
import com.softawii.curupira.annotations.IGroup;
import dev.softawii.entity.Student;
import dev.softawii.service.StudentService;
import io.micronaut.context.annotation.Context;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Optional;

@IGroup(name = "student", description = "Comandos para exibir informações sobre os estudantes")
@Context
public class StudentInformationController {

    private static StudentService studentService;

    public StudentInformationController(StudentService studentService) {
        StudentInformationController.studentService = studentService;
    }

    @ICommand(name = "info", description = "Exibe informações sobre o estudante que executou o comando")
    public static void info(SlashCommandInteractionEvent event) {
        Optional<Student> student = studentService.findByDiscordId(event.getUser().getIdLong());

        if (student.isEmpty()) {
            event.reply("Você não está registrado.").setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(studentService.getStudentInfo(event.getUser(), student.get())).setEphemeral(true).queue();
    }
}
