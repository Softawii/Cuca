package dev.softawii.entity;

import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity
public class AuthenticationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Maybe an attacker tries to create a token for a student that doesn't exist
     * so that information is not unique...
     */
    @Column(nullable = false, unique = false)
    private String email;

    @Column(nullable = false, unique = false)
    private Long discordUserId;

    @Column(nullable = false, unique = false)
    private String token;

    @Column
    private ZonedDateTime createdAt;

    @Column
    private ZonedDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used = Boolean.FALSE;

    //region Constructors
    public AuthenticationToken(Long discordUserId, String email, String token, ZonedDateTime createdAt, ZonedDateTime expiresAt) {
        this.discordUserId = discordUserId;
        this.email = email;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public AuthenticationToken() {
    }
    //endregion

    //region Getters and Setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getDiscordUserId() {
        return discordUserId;
    }

    public void setDiscordUserId(Long discordUserId) {
        this.discordUserId = discordUserId;
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
                "id=" + id +
                ", email='" + email + '\'' +
                ", discordUserId=" + discordUserId +
                ", token='" + token + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                '}';
    }
}
