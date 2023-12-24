package dev.softawii.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class DynamicConfig {

    @Id
    private Long id; // must be 0

    @Column
    private Long eventChannelId;

    @Column
    private Long joinServerRole;

    @Column
    private Long verifiedRoleId;

    public DynamicConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventChannelId() {
        return eventChannelId;
    }

    public void setEventChannelId(Long adminEventChannelId) {
        this.eventChannelId = adminEventChannelId;
    }

    public Long getJoinServerRole() {
        return joinServerRole;
    }

    public void setJoinServerRole(Long onServerEnterRoleId) {
        this.joinServerRole = onServerEnterRoleId;
    }

    public Long getVerifiedRoleId() {
        return verifiedRoleId;
    }

    public void setVerifiedRoleId(Long onUserVerifiedRoleId) {
        this.verifiedRoleId = onUserVerifiedRoleId;
    }

    @Override
    public String toString() {
        return "DynamicConfig{" +
                "id=" + id +
                ", eventChannelId=" + eventChannelId +
                ", joinServerRole=" + joinServerRole +
                ", verifiedRoleId=" + verifiedRoleId +
                '}';
    }
}
