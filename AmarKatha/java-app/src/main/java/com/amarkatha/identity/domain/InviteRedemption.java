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
@Table(name = "invite_redemption")
public class InviteRedemption {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invite_token_id", nullable = false)
    private InviteToken inviteToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt;

    protected InviteRedemption() {
    }

    public static InviteRedemption create(InviteToken inviteToken, User user, Instant redeemedAt) {
        InviteRedemption redemption = new InviteRedemption();
        redemption.id = UUID.randomUUID();
        redemption.inviteToken = inviteToken;
        redemption.user = user;
        redemption.redeemedAt = redeemedAt;
        return redemption;
    }

    public UUID getId() {
        return id;
    }

    public InviteToken getInviteToken() {
        return inviteToken;
    }

    public User getUser() {
        return user;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }
}
