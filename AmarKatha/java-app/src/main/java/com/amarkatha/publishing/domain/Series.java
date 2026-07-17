package com.amarkatha.publishing.domain;

import com.amarkatha.shared.domain.SeriesCadence;
import com.amarkatha.shared.domain.SeriesStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "series")
public class Series {

    @Id
    private UUID id;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_storage_key")
    private String coverStorageKey;

    @Column(name = "content_language", nullable = false, length = 16)
    private String contentLanguage = "en";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> genres = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeriesStatus status = SeriesStatus.ONGOING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeriesCadence cadence = SeriesCadence.OFF;

    @Column(name = "day_of_week")
    private Short dayOfWeek;

    @Column(name = "period_days")
    private Short periodDays;

    @Column(name = "release_hour_ist")
    private Short releaseHourIst;

    @Column(name = "next_expected_at")
    private Instant nextExpectedAt;

    @Column(name = "last_published_at")
    private Instant lastPublishedAt;

    @Column(name = "skip_message", length = 280)
    private String skipMessage;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Series() {
    }

    public static Series create(UUID creatorId, String slug, String title) {
        Series series = new Series();
        series.id = UUID.randomUUID();
        series.creatorId = creatorId;
        series.slug = slug;
        series.title = title;
        return series;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverStorageKey() {
        return coverStorageKey;
    }

    public String getContentLanguage() {
        return contentLanguage;
    }

    public List<String> getGenres() {
        return genres;
    }

    public SeriesStatus getStatus() {
        return status;
    }

    public SeriesCadence getCadence() {
        return cadence;
    }

    public Short getDayOfWeek() {
        return dayOfWeek;
    }

    public Short getPeriodDays() {
        return periodDays;
    }

    public Short getReleaseHourIst() {
        return releaseHourIst;
    }

    public Instant getNextExpectedAt() {
        return nextExpectedAt;
    }

    public Instant getLastPublishedAt() {
        return lastPublishedAt;
    }

    public String getSkipMessage() {
        return skipMessage;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres != null ? new ArrayList<>(genres) : new ArrayList<>();
    }

    public void setCoverStorageKey(String coverStorageKey) {
        this.coverStorageKey = coverStorageKey;
    }

    public void setStatus(SeriesStatus status) {
        this.status = status;
    }

    public void setLastPublishedAt(Instant lastPublishedAt) {
        this.lastPublishedAt = lastPublishedAt;
    }

    public void applySchedule(
            SeriesStatus status,
            SeriesCadence cadence,
            Integer periodDays,
            Short dayOfWeek,
            Short releaseHourIst,
            Instant nextExpectedAt,
            Instant lastPublishedAt,
            String skipMessage
    ) {
        this.status = status;
        this.cadence = cadence;
        this.periodDays = periodDays == null ? null : periodDays.shortValue();
        this.dayOfWeek = dayOfWeek;
        this.releaseHourIst = releaseHourIst;
        this.nextExpectedAt = nextExpectedAt;
        this.lastPublishedAt = lastPublishedAt;
        this.skipMessage = skipMessage;
    }
}
