package com.amarkatha.identity;

import com.amarkatha.identity.domain.InviteToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {

    Optional<InviteToken> findByToken(String token);

    List<InviteToken> findAllByOrderByCreatedAtDesc();

    @Query("select i from InviteToken i left join fetch i.createdBy order by i.createdAt desc")
    List<InviteToken> findAllWithCreatedByOrderByCreatedAtDesc();

    @Query("""
            select count(i) from InviteToken i
            where i.useCount < i.maxUses
              and (i.expiresAt is null or i.expiresAt > :now)
            """)
    long countAvailable(@Param("now") Instant now);

    @Query("select count(i) from InviteToken i where i.usedAt is not null and i.usedAt >= :since")
    long countUsedSince(@Param("since") Instant since);

    /**
     * Atomically redeem one use. Returns 1 if consumed, 0 if exhausted/expired/missing.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE invite_token
            SET use_count = use_count + 1,
                used_by = :userId,
                used_at = :now
            WHERE id = :id
              AND use_count < max_uses
              AND (expires_at IS NULL OR expires_at > :now)
            """, nativeQuery = true)
    int tryConsume(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );
}
