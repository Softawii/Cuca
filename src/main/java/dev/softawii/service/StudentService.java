package dev.softawii.service;

import dev.softawii.entity.Student;
import dev.softawii.repository.StudentRepository;
import dev.softawii.util.EmbedUtil;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.Optional;

@Singleton
public class StudentService {

    private final StudentRepository repository;
    private final EventService eventService;
    private final EmbedUtil embedUtil;

    public StudentService(StudentRepository repository, EventService eventService, EmbedUtil embedUtil) {
        this.repository = repository;
        this.eventService = eventService;
        this.embedUtil = embedUtil;
    }

    public boolean alreadySetup(Long discordId) {
        return repository.existsByDiscordUserId(discordId);
    }

    public void removeStudent(Long discordId, User user) {
        if (!repository.existsByDiscordUserId(discordId)) return;
        repository.deleteByDiscordUserId(discordId);
        eventService.dispatch(embedUtil.generate( EmbedUtil.EmbedLevel.WARNING, "Usuário removido", "Usuário " + user.getGlobalName() + " removido e inválidado pelo bot", null, null);
    }

    public MessageEmbed getStudentInfo(User user, Student student) {
        return embedUtil.generate(
                EmbedUtil.EmbedLevel.INFO,
                "Informações do usuário",
                "Informações do usuário " + student.getDiscordUserId(),
                null,
                new EmbedUtil.Author(user.getName(), user.getEffectiveAvatarUrl()),
                new MessageEmbed.Field("Email", student.getEmail(), false),
                new MessageEmbed.Field("Discord ID", student.getDiscordUserId().toString(), false)
        );
    }

    public Optional<Student> findByDiscordId(Long discordId) {
        return repository.findByDiscordUserId(discordId);
    }

    public Student createStudent(Long discordId, String email) {
        Student student = new Student(discordId, email);
        return repository.saveAndFlush(student);
    }

    public Optional<Student> findByEmail(String email) {
        return repository.findByEmail(email);
    }
}
