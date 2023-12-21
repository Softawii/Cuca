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

    public void setDiscordId(Long discordId) {
        this.discordId = discordId;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(ZonedDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    //endregion
}
