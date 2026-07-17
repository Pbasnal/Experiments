package com.amarkatha.identity;

import com.amarkatha.identity.domain.InviteToken;
import com.amarkatha.identity.domain.User;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteService {

    private static final Duration DEFAULT_EXPIRY = Duration.ofDays(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InviteTokenRepository inviteTokenRepository;

    public InviteService(InviteTokenRepository inviteTokenRepository) {
        this.inviteTokenRepository = inviteTokenRepository;
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
        if (invite.isUsed()) {
            throw new InviteInvalidException(InviteInvalidException.InviteInvalidReason.ALREADY_USED);
        }
        if (invite.isExpired(now)) {
            throw new InviteInvalidException(InviteInvalidException.InviteInvalidReason.EXPIRED);
        }
        return invite;
    }

    @Transactional
    public InviteToken consume(String rawToken, User usedBy) {
        InviteToken invite = resolveUsable(rawToken);
        invite.markUsed(usedBy, Instant.now());
        return inviteTokenRepository.save(invite);
    }

    @Transactional
    public InviteToken generate(User createdBy) {
        return generate(createdBy, DEFAULT_EXPIRY);
    }

    @Transactional
    public InviteToken generate(User createdBy, Duration expiry) {
        String token = randomToken();
        while (inviteTokenRepository.findByToken(token).isPresent()) {
            token = randomToken();
        }
        Instant expiresAt = expiry != null ? Instant.now().plus(expiry) : null;
        InviteToken invite = InviteToken.create(token, createdBy, expiresAt);
        return inviteTokenRepository.save(invite);
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
