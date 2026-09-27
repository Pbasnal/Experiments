package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.NotificationEmailDelivery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationEmailDeliveryRepository extends JpaRepository<NotificationEmailDelivery, UUID> {

    Optional<NotificationEmailDelivery> findByDedupeKey(String dedupeKey);

    List<NotificationEmailDelivery> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            NotificationEmailDelivery.Status status,
            Instant nextAttemptAt
    );

    /**
     * Atomically claim a PENDING row for processing. {@code leaseUntil} is stored in
     * {@code nextAttemptAt} while status is PROCESSING so crashed workers can recover
     * after the lease expires.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationEmailDelivery d
               set d.status = com.amarkatha.engagement.domain.NotificationEmailDelivery.Status.PROCESSING,
                   d.nextAttemptAt = :leaseUntil
             where d.id = :id
               and d.status = com.amarkatha.engagement.domain.NotificationEmailDelivery.Status.PENDING
            """)
    int claimPending(@Param("id") UUID id, @Param("leaseUntil") Instant leaseUntil);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationEmailDelivery d
               set d.status = com.amarkatha.engagement.domain.NotificationEmailDelivery.Status.PENDING,
                   d.nextAttemptAt = :now
             where d.status = com.amarkatha.engagement.domain.NotificationEmailDelivery.Status.PROCESSING
               and d.nextAttemptAt < :now
            """)
    int recoverStaleProcessing(@Param("now") Instant now);
}
