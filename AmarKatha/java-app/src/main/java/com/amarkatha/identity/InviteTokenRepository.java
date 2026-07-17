package com.amarkatha.identity;

import com.amarkatha.identity.domain.InviteToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {

    Optional<InviteToken> findByToken(String token);

    List<InviteToken> findAllByOrderByCreatedAtDesc();

    @Query("select i from InviteToken i left join fetch i.createdBy order by i.createdAt desc")
    List<InviteToken> findAllWithCreatedByOrderByCreatedAtDesc();

    @Query("""
            select count(i) from InviteToken i
            where i.usedAt is null
              and (i.expiresAt is null or i.expiresAt > :now)
            """)
    long countAvailable(@Param("now") Instant now);

    @Query("select count(i) from InviteToken i where i.usedAt is not null and i.usedAt >= :since")
    long countUsedSince(@Param("since") Instant since);
}
