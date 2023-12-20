package dev.softawii.service;

import dev.softawii.repository.StudentRepository;
import jakarta.inject.Singleton;

@Singleton
public class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public boolean alreadySetup(Long discordId) {
        return repository.existsByDiscordUserId(discordId);
    }
}
