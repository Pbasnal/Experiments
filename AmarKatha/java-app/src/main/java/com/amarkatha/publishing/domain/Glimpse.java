package com.amarkatha.publishing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "glimpse")
public class Glimpse {

    @Id
    private UUID id;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(nullable = false, length = 32)
    private String tag;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "copyright_ack_at", nullable = false)
    private Instant copyrightAckAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Glimpse() {
    }

    public static Glimpse create(UUID id, UUID seriesId, String tag, Instant postedAt) {
        Glimpse glimpse = new Glimpse();
        glimpse.id = id;
        glimpse.seriesId = seriesId;
        glimpse.tag = tag;
        glimpse.postedAt = postedAt;
        glimpse.copyrightAckAt = postedAt;
        glimpse.createdAt = postedAt;
        return glimpse;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public String getTag() {
        return tag;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public Instant getCopyrightAckAt() {
        return copyrightAckAt;
    }
}
