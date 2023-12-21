package dev.softawii.entity;

import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity
public class AuthenticationToken {

    @Id
    private Long discordId;
    @ManyToOne
    private Student student;
    @Column(nullable = false, unique = false)
    private String token;
    @Column
    private ZonedDateTime createdAt;
    @Column
    private ZonedDateTime expiresAt;
    @Column
    private Boolean used;

    //region Constructors
    public AuthenticationToken(Long discordId, Student student, String token, ZonedDateTime createdAt, ZonedDateTime expiresAt) {
        this.discordId = discordId;
        this.student = student;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public AuthenticationToken() {
    }
    //endregion

    //region Getters and Setters
    public Long getDiscordId() {
        return discordId;
    }

    public Student getStudent() {
        return student;
    }

    public String getToken() {
        return token;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }

    public Boolean getUsed() {
        return used;
    }
    //endregion
}
