package dev.softawii.service;

import dev.softawii.entity.Student;
import dev.softawii.repository.StudentRepository;
import dev.softawii.util.EmbedUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.netty.util.internal.StringUtil;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
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
        eventService.dispatch(embedUtil.generate( EmbedUtil.EmbedLevel.WARNING, "Usuário removido", "Usuário " + user.getGlobalName() + " removido e inválidado pelo bot", null, null));
    }

    public MessageEmbed getStudentInfo(User user, Student student) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss O");
        return embedUtil.generate(
                EmbedUtil.EmbedLevel.INFO,
                "Informações do usuário",
                "Informações do usuário " + student.getDiscordUserId(),
                null,
                new EmbedUtil.Author(user.getName(), user.getEffectiveAvatarUrl()),
                new MessageEmbed.Field("Email", student.getEmail(), false),
                new MessageEmbed.Field("Discord ID", student.getDiscordUserId().toString(), false),
                new MessageEmbed.Field("Data de registro", student.getCreatedAt().format(formatter), false),
                new MessageEmbed.Field("Data de atualização", student.getUpdatedAt().format(formatter), false)
        );
    }

    public Optional<Student> findByDiscordId(Long discordId) {
        return repository.findByDiscordUserId(discordId);
    }

    public void createStudent(Long discordId, String email) {
        Student student = new Student(discordId, email);
        repository.saveAndFlush(student);
    }

    public Optional<Student> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Page<Student> findAll(int page, int size) {
        return repository.findAll(Pageable.from(page, size));
    }

    public MessageEmbed getStudentListEmbed(Page<Student> page) {
        String pageCount = (page.getPageNumber() + 1) + "/" + page.getTotalPages();
        String students = page.getContent().stream()
                .map(student -> String.format("<@!%s> - %s", student.getDiscordUserId(), student.getEmail()))
                .reduce("", (a, b) -> a + "\n" + b);

        return embedUtil.generate(
                EmbedUtil.EmbedLevel.INFO,
                "List of students",
                "List of all registered students",
                "Pages: " + pageCount,
                null,
                new MessageEmbed.Field("Total Elements", String.valueOf(page.getTotalSize()), true),
                new MessageEmbed.Field("Page Size", String.valueOf(page.getSize()), true),
                new MessageEmbed.Field("Students", students, false)
        );
    }

    public Collection<ActionComponent> getActionRow(Page<Student> page) {
        return List.of(
            // Button.primary("list-page:0", "First Page").withDisabled(page.getPageNumber() == 0),
            Button.primary("list-page:" + (page.getPageNumber() - 1), "Previous Page").withDisabled(page.getPageNumber() == 0),
            Button.primary("list-page:" + (page.getPageNumber() + 1), "Next Page").withDisabled(page.getPageNumber() == page.getTotalPages() - 1)
            // Button.primary("list-page:" + page.getTotalPages(), "Last Page").withDisabled(page.getPageNumber() == page.getTotalPages() - 1)
        );
    }
}
