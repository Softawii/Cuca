package dev.softawii.entity;

import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity
public class AuthenticationToken {

    @ManyToOne
    @Id
    @JoinColumn
    private Student student;

    @Column(nullable = false, unique = false)
    private String token;

    @Column
    private ZonedDateTime createdAt;

    @Column
    private ZonedDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used = Boolean.FALSE;

    //region Constructors
    public AuthenticationToken(Student student, String token, ZonedDateTime createdAt, ZonedDateTime expiresAt) {
        this.student = student;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public AuthenticationToken() {
    }
    //endregion

    //region Getters and Setters

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

    @Override
    public String toString() {
        return "AuthenticationToken{" +
               "student=" + student +
               ", token='" + token + '\'' +
               ", createdAt=" + createdAt +
               ", expiresAt=" + expiresAt +
               ", used=" + used +
               '}';
    }
}
