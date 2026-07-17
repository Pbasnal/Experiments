package com.amarkatha.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_event")
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnalyticsEventType type;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "chapter_id")
    private UUID chapterId;

    @Column(name = "reader_id", nullable = false, length = 36)
    private String readerId;

    @Column(nullable = false, length = 32)
    private String referrer;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected AnalyticsEvent() {
    }

    public static AnalyticsEvent create(
            AnalyticsEventType type,
            UUID seriesId,
            UUID chapterId,
            String readerId,
            String referrer
    ) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.type = type;
        event.seriesId = seriesId;
        event.chapterId = chapterId;
        event.readerId = readerId;
        event.referrer = referrer;
        event.occurredAt = Instant.now();
        return event;
    }

    public Long getId() {
        return id;
    }

    public AnalyticsEventType getType() {
        return type;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public UUID getChapterId() {
        return chapterId;
    }

    public String getReaderId() {
        return readerId;
    }

    public String getReferrer() {
        return referrer;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
