package dev.softawii.service;

import dev.softawii.entity.Student;
import dev.softawii.repository.StudentRepository;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public boolean alreadySetup(Long discordId) {
        return repository.existsByDiscordUserId(discordId);
    }

    public Optional<Student> findByDiscordId(Long discordId) {
        return repository.findByDiscordUserId(discordId);
    }

    public Student getOrCreate(Long discordId, String email) {
        return repository.findByDiscordUserId(discordId).orElseGet(() -> createStudent(discordId, email));
    }

    public Student createStudent(Long discordId, String email) {
        Student student = new Student(discordId, email);
        return repository.saveAndFlush(student);
    }

    public Optional<Student> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public void verifyStudent(Long discordId) {
        repository.verifyStudentByDiscordUserId(discordId);
    }
}
