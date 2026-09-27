package com.amarkatha.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DomainEventOutboxRepository extends JpaRepository<DomainEventOutbox, UUID> {

    Optional<DomainEventOutbox> findByDedupeKey(String dedupeKey);

    List<DomainEventOutbox> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus status,
            Instant nextAttemptAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DomainEventOutbox e
               set e.status = com.amarkatha.outbox.OutboxEventStatus.PROCESSING
             where e.id = :id
               and e.status = com.amarkatha.outbox.OutboxEventStatus.PENDING
            """)
    int claimPending(@Param("id") UUID id);
}
