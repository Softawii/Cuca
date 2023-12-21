package dev.softawii.repository;

import dev.softawii.entity.AuthenticationToken;
import dev.softawii.entity.Student;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

@Repository
public interface AuthenticationTokenRepository extends JpaRepository<AuthenticationToken, Student> {

    @Query("""
            select count(at) > 0 from AuthenticationToken at
            where
                at.expiresAt <= current_timestamp and
                at.used = false and
                at.student.discordUserId = :discordId
            """)
    boolean validTokenExists(Long discordId);
}
