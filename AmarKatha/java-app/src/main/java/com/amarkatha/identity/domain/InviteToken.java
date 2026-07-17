package com.amarkatha.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite_token")
public class InviteToken {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    private User usedBy;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected InviteToken() {
    }

    public static InviteToken create(String token, User createdBy, Instant expiresAt) {
        InviteToken invite = new InviteToken();
        invite.id = UUID.randomUUID();
        invite.token = token;
        invite.createdBy = createdBy;
        invite.expiresAt = expiresAt;
        return invite;
    }

    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getUsedBy() {
        return usedBy;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public void markUsed(User user, Instant now) {
        this.usedBy = user;
        this.usedAt = now;
    }
}
