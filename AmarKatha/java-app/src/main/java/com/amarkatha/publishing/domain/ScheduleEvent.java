package com.amarkatha.publishing.domain;

import com.amarkatha.scheduling.ScheduleEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_event")
public class ScheduleEvent {

    @Id
    private UUID id;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private ScheduleEventType eventType;

    @Column(length = 280)
    private String message;

    @Column(name = "previous_next_expected_at")
    private Instant previousNextExpectedAt;

    @Column(name = "new_next_expected_at")
    private Instant newNextExpectedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ScheduleEvent() {
    }

    public static ScheduleEvent create(
            UUID seriesId,
            ScheduleEventType eventType,
            String message,
            Instant previousNextExpectedAt,
            Instant newNextExpectedAt,
            UUID createdBy
    ) {
        ScheduleEvent event = new ScheduleEvent();
        event.id = UUID.randomUUID();
        event.seriesId = seriesId;
        event.eventType = eventType;
        event.message = message;
        event.previousNextExpectedAt = previousNextExpectedAt;
        event.newNextExpectedAt = newNextExpectedAt;
        event.createdBy = createdBy;
        return event;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public ScheduleEventType getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public Instant getPreviousNextExpectedAt() {
        return previousNextExpectedAt;
    }

    public Instant getNewNextExpectedAt() {
        return newNextExpectedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
