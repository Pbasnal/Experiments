package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.NotificationEmailDelivery;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Delivers queued notification emails with <strong>at-least-once</strong> semantics.
 *
 * <p>Each claim is committed before SMTP runs so a crash mid-send does not leave the row
 * stuck in {@code PENDING} forever. Completion and retry persistence run in separate
 * transactions after the side effect. A crash between a successful SMTP send and
 * {@code markSent} can still duplicate delivery when the lease expires and the row is
 * retried — do not claim exactly-once. The outbox {@code dedupe_key} prevents duplicate
 * queue rows; SMTP itself is not idempotent.
 *
 * <p>Stale {@code PROCESSING} rows (lease expired after a worker crash) are recovered
 * back to {@code PENDING} before each batch.
 */
@Service
public class NotificationEmailDeliveryProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailDeliveryProcessor.class);

    private final NotificationEmailDeliveryRepository deliveryRepository;
    private final NotificationMailSender mailSender;
    private final NotificationProperties properties;
    private final NotificationMetrics metrics;
    private final TransactionTemplate transactionTemplate;

    public NotificationEmailDeliveryProcessor(
            NotificationEmailDeliveryRepository deliveryRepository,
            NotificationMailSender mailSender,
            NotificationProperties properties,
            NotificationMetrics metrics,
            PlatformTransactionManager transactionManager
    ) {
        this.deliveryRepository = deliveryRepository;
        this.mailSender = mailSender;
        this.properties = properties;
        this.metrics = metrics;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int processPendingBatch() {
        if (!mailSender.isEnabled()) {
            return 0;
        }
        Instant now = Instant.now();
        recoverStaleProcessing(now);

        List<NotificationEmailDelivery> pending = transactionTemplate.execute(status ->
                deliveryRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        NotificationEmailDelivery.Status.PENDING,
                        now
                )
        );
        if (pending == null || pending.isEmpty()) {
            return 0;
        }

        Instant leaseUntil = now.plus(Duration.ofMillis(properties.emailProcessingLeaseMs()));
        int processed = 0;
        for (NotificationEmailDelivery delivery : pending) {
            UUID id = delivery.getId();
            boolean claimed = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                    deliveryRepository.claimPending(id, leaseUntil) > 0
            ));
            if (!claimed) {
                continue;
            }

            NotificationEmailDelivery claimedRow = transactionTemplate.execute(status ->
                    deliveryRepository.findById(id).orElse(null)
            );
            if (claimedRow == null) {
                continue;
            }

            try {
                // Side effect outside any DB transaction.
                mailSender.send(claimedRow.getToEmail(), claimedRow.getSubject(), claimedRow.getBodyText());
                transactionTemplate.executeWithoutResult(status -> {
                    NotificationEmailDelivery row = deliveryRepository.findById(id).orElse(null);
                    if (row == null) {
                        return;
                    }
                    row.markSent(Instant.now());
                    deliveryRepository.save(row);
                });
                metrics.emailSent();
                processed++;
            } catch (Exception ex) {
                log.warn("Email delivery {} failed: {}", id, ex.getMessage());
                transactionTemplate.executeWithoutResult(status -> {
                    NotificationEmailDelivery row = deliveryRepository.findById(id).orElse(null);
                    if (row == null) {
                        return;
                    }
                    Instant next = Instant.now().plus(ChapterPublishedOutboxProcessor.backoff(row.getAttemptCount() + 1));
                    row.scheduleRetry(next, ex.getMessage(), properties.maxAttempts());
                    deliveryRepository.save(row);
                    if (row.getStatus() == NotificationEmailDelivery.Status.FAILED) {
                        metrics.emailFailed();
                    } else {
                        metrics.emailRetried();
                    }
                });
            }
        }
        return processed;
    }

    private void recoverStaleProcessing(Instant now) {
        Integer recovered = transactionTemplate.execute(status ->
                deliveryRepository.recoverStaleProcessing(now)
        );
        if (recovered != null && recovered > 0) {
            log.info("Recovered {} stale PROCESSING email delivery row(s)", recovered);
        }
    }
}
