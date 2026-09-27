package com.amarkatha.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "series_follow")
public class SeriesFollow {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected SeriesFollow() {
    }

    public static SeriesFollow create(UUID userId, UUID seriesId) {
        SeriesFollow follow = new SeriesFollow();
        follow.id = UUID.randomUUID();
        follow.userId = userId;
        follow.seriesId = seriesId;
        follow.createdAt = Instant.now();
        return follow;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
