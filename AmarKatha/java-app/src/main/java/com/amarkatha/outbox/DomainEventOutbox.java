package com.amarkatha.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "domain_event_outbox")
public class DomainEventOutbox {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "dedupe_key", nullable = false, length = 255)
    private String dedupeKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    protected DomainEventOutbox() {
    }

    public static DomainEventOutbox pending(
            String eventType,
            String aggregateType,
            UUID aggregateId,
            String dedupeKey,
            Map<String, Object> payload
    ) {
        DomainEventOutbox event = new DomainEventOutbox();
        event.id = UUID.randomUUID();
        event.eventType = eventType;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.dedupeKey = dedupeKey;
        event.payload = payload;
        event.status = OutboxEventStatus.PENDING;
        event.attemptCount = 0;
        event.nextAttemptAt = Instant.now();
        event.createdAt = Instant.now();
        return event;
    }

    public void markProcessed(Instant now) {
        this.status = OutboxEventStatus.PROCESSED;
        this.processedAt = now;
        this.lastError = null;
    }

    public void scheduleRetry(Instant nextAttemptAt, String error, int maxAttempts) {
        this.attemptCount += 1;
        this.lastError = truncate(error, 2000);
        this.nextAttemptAt = nextAttemptAt;
        if (this.attemptCount >= maxAttempts) {
            this.status = OutboxEventStatus.FAILED;
        } else {
            this.status = OutboxEventStatus.PENDING;
        }
    }

    public void markClaimed() {
        this.status = OutboxEventStatus.PROCESSING;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
