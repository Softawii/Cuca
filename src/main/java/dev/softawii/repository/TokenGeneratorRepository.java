package dev.softawii.repository;

import dev.softawii.entity.AuthenticationToken;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface TokenGeneratorRepository extends JpaRepository<AuthenticationToken, Long> {

    @Query("""
            select count(at) > 0 from AuthenticationToken at
            where
                at.expiresAt <= current_timestamp and
                at.used = false and
                at.student.discordUserId = :discordId
            order by
                at.createdAt desc
            """)
    boolean validTokenExists(Long discordId);
}
