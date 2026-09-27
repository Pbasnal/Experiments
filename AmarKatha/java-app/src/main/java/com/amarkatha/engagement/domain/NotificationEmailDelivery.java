package com.amarkatha.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_email_delivery")
public class NotificationEmailDelivery {

    public enum Status {
        PENDING,
        PROCESSING,
        SENT,
        FAILED,
        SKIPPED
    }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "dedupe_key", nullable = false, length = 255)
    private String dedupeKey;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "body_text", nullable = false, columnDefinition = "TEXT")
    private String bodyText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    protected NotificationEmailDelivery() {
    }

    public static NotificationEmailDelivery pending(
            UUID userId,
            UUID notificationId,
            String dedupeKey,
            String toEmail,
            String subject,
            String bodyText
    ) {
        NotificationEmailDelivery delivery = new NotificationEmailDelivery();
        delivery.id = UUID.randomUUID();
        delivery.userId = userId;
        delivery.notificationId = notificationId;
        delivery.dedupeKey = dedupeKey;
        delivery.toEmail = toEmail;
        delivery.subject = subject;
        delivery.bodyText = bodyText;
        delivery.status = Status.PENDING;
        delivery.attemptCount = 0;
        delivery.nextAttemptAt = Instant.now();
        delivery.createdAt = Instant.now();
        return delivery;
    }

    public void markSent(Instant now) {
        this.status = Status.SENT;
        this.sentAt = now;
        this.lastError = null;
    }

    public void markSkipped(String reason) {
        this.status = Status.SKIPPED;
        this.lastError = truncate(reason, 2000);
    }

    public void scheduleRetry(Instant nextAttemptAt, String error, int maxAttempts) {
        this.attemptCount += 1;
        this.lastError = truncate(error, 2000);
        this.nextAttemptAt = nextAttemptAt;
        if (this.attemptCount >= maxAttempts) {
            this.status = Status.FAILED;
        } else {
            this.status = Status.PENDING;
        }
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

    public UUID getUserId() {
        return userId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public String getToEmail() {
        return toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyText() {
        return bodyText;
    }

    public Status getStatus() {
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

    public Instant getSentAt() {
        return sentAt;
    }
}
