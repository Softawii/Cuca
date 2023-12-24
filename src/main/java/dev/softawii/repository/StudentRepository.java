package dev.softawii.repository;


import dev.softawii.entity.Student;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByDiscordUserId(Long discordUserId);
    Optional<Student> findByDiscordUserId(Long discordUserId);
    Optional<Student> findByEmail(String email);
    void deleteByDiscordUserId(Long discordId);

    Page<Student> findAll(Pageable request);
}
