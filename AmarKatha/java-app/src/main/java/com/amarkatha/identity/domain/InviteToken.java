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

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    @Column(name = "use_count", nullable = false)
    private int useCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected InviteToken() {
    }

    public static InviteToken create(String token, User createdBy, Instant expiresAt, int maxUses) {
        InviteToken invite = new InviteToken();
        invite.id = UUID.randomUUID();
        invite.token = token;
        invite.createdBy = createdBy;
        invite.expiresAt = expiresAt;
        invite.maxUses = maxUses;
        invite.useCount = 0;
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

    public int getMaxUses() {
        return maxUses;
    }

    public int getUseCount() {
        return useCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** True when all allowed redemptions have been consumed. */
    public boolean isExhausted() {
        return useCount >= maxUses;
    }

    /** @deprecated Prefer {@link #isExhausted()} for multi-use tokens. */
    @Deprecated
    public boolean isUsed() {
        return isExhausted();
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public int remainingUses() {
        return Math.max(0, maxUses - useCount);
    }

    /** Apply an atomic consume result into this in-memory entity (after successful DB update). */
    public void applyConsumed(User user, Instant now) {
        this.useCount = this.useCount + 1;
        this.usedBy = user;
        this.usedAt = now;
    }
}
