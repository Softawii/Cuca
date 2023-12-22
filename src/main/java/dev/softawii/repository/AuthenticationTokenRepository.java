package dev.softawii.repository;

import dev.softawii.entity.AuthenticationToken;
import dev.softawii.entity.Student;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface AuthenticationTokenRepository extends JpaRepository<AuthenticationToken, Student> {

    @Query("""
            select count(at) > 0 from AuthenticationToken at
            where
                at.expiresAt >= current_timestamp and
                at.used = false and
                at.student.discordUserId = :discordId
            """)
    boolean validTokenExists(Long discordId);

    @Query("""
            select at from AuthenticationToken at
            where
                at.expiresAt >= current_timestamp and
                at.used = false and
                at.student.discordUserId = :discordId and
                at.token = :token
            """)
    Optional<AuthenticationToken> findValidToken(String token, Long discordId);
}
