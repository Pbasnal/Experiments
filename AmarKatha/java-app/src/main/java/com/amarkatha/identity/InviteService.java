package com.amarkatha.identity;

import com.amarkatha.identity.domain.InviteRedemption;
import com.amarkatha.identity.domain.InviteToken;
import com.amarkatha.identity.domain.User;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteService {

    private static final Duration DEFAULT_EXPIRY = Duration.ofDays(30);
    private static final int MIN_MAX_USES = 1;
    private static final int MAX_MAX_USES = 100;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InviteTokenRepository inviteTokenRepository;
    private final InviteRedemptionRepository inviteRedemptionRepository;

    public InviteService(
            InviteTokenRepository inviteTokenRepository,
            InviteRedemptionRepository inviteRedemptionRepository
    ) {
        this.inviteTokenRepository = inviteTokenRepository;
        this.inviteRedemptionRepository = inviteRedemptionRepository;
    }

    @Transactional(readOnly = true)
    public void validateForSignup(String rawToken) {
        resolveUsable(rawToken);
    }

    @Transactional(readOnly = true)
    public InviteToken resolveUsable(String rawToken) {
        String token = normalize(rawToken);
        if (token.isEmpty()) {
            throw new InviteInvalidException(InviteInvalidException.InviteInvalidReason.BLANK);
        }
        InviteToken invite = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new InviteInvalidException(InviteInvalidException.InviteInvalidReason.NOT_FOUND));
        Instant now = Instant.now();
        if (invite.isExhausted()) {
            throw new InviteInvalidException(InviteInvalidException.InviteInvalidReason.EXHAUSTED);
        }
        if (invite.isExpired(now)) {
            throw new InviteInvalidException(InviteInvalidException.InviteInvalidReason.EXPIRED);
        }
        return invite;
    }

    @Transactional
    public InviteToken consume(String rawToken, User usedBy) {
        InviteToken invite = resolveUsable(rawToken);
        Instant now = Instant.now();
        int updated = inviteTokenRepository.tryConsume(invite.getId(), usedBy.getId(), now);
        if (updated != 1) {
            throw new InviteInvalidException(InviteInvalidException.InviteInvalidReason.EXHAUSTED);
        }
        invite.applyConsumed(usedBy, now);
        inviteRedemptionRepository.save(InviteRedemption.create(invite, usedBy, now));
        return invite;
    }

    @Transactional(readOnly = true)
    public List<InviteRedemption> listRedemptionsForInvites(Collection<UUID> inviteIds) {
        if (inviteIds == null || inviteIds.isEmpty()) {
            return List.of();
        }
        return inviteRedemptionRepository.findByInviteTokenIdInWithUser(inviteIds);
    }

    @Transactional
    public InviteToken generate(User createdBy) {
        return generate(createdBy, DEFAULT_EXPIRY, 1);
    }

    @Transactional
    public InviteToken generate(User createdBy, int maxUses) {
        return generate(createdBy, DEFAULT_EXPIRY, maxUses);
    }

    @Transactional
    public InviteToken generate(User createdBy, Duration expiry) {
        return generate(createdBy, expiry, 1);
    }

    @Transactional
    public InviteToken generate(User createdBy, Duration expiry, int maxUses) {
        if (maxUses < MIN_MAX_USES || maxUses > MAX_MAX_USES) {
            throw new IllegalArgumentException(
                    "Max uses must be between " + MIN_MAX_USES + " and " + MAX_MAX_USES + "."
            );
        }
        String token = randomToken();
        while (inviteTokenRepository.findByToken(token).isPresent()) {
            token = randomToken();
        }
        Instant expiresAt = expiry != null ? Instant.now().plus(expiry) : null;
        InviteToken invite = InviteToken.create(token, createdBy, expiresAt, maxUses);
        return inviteTokenRepository.save(invite);
    }

    public static int minMaxUses() {
        return MIN_MAX_USES;
    }

    public static int maxMaxUses() {
        return MAX_MAX_USES;
    }

    @Transactional(readOnly = true)
    public List<InviteToken> listRecent() {
        return inviteTokenRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<InviteToken> listRecentWithCreator() {
        return inviteTokenRepository.findAllWithCreatedByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countAvailable() {
        return inviteTokenRepository.countAvailable(Instant.now());
    }

    @Transactional(readOnly = true)
    public long countUsedSince(Instant since) {
        return inviteTokenRepository.countUsedSince(since);
    }

    private static String normalize(String rawToken) {
        return rawToken == null ? "" : rawToken.trim();
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
