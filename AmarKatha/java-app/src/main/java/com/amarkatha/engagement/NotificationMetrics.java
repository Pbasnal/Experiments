package com.amarkatha.engagement;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final Counter notificationsCreated;
    private final Counter emailQueued;
    private final Counter emailSent;
    private final Counter emailFailed;
    private final Counter emailRetried;
    private final Counter emailSkipped;
    private final Counter outboxProcessed;
    private final Counter outboxFailed;
    private final Counter outboxRetried;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.notificationsCreated = Counter.builder("amarkatha.notifications.created")
                .description("In-app notifications created")
                .register(meterRegistry);
        this.emailQueued = Counter.builder("amarkatha.notifications.email.queued")
                .description("Notification emails queued")
                .register(meterRegistry);
        this.emailSent = Counter.builder("amarkatha.notifications.email.sent")
                .description("Notification emails sent")
                .register(meterRegistry);
        this.emailFailed = Counter.builder("amarkatha.notifications.email.failed")
                .description("Notification emails permanently failed")
                .register(meterRegistry);
        this.emailRetried = Counter.builder("amarkatha.notifications.email.retried")
                .description("Notification email transient retries")
                .register(meterRegistry);
        this.emailSkipped = Counter.builder("amarkatha.notifications.email.skipped")
                .description("Notification emails skipped (mail disabled)")
                .register(meterRegistry);
        this.outboxProcessed = Counter.builder("amarkatha.outbox.processed")
                .description("Domain outbox events processed")
                .register(meterRegistry);
        this.outboxFailed = Counter.builder("amarkatha.outbox.failed")
                .description("Domain outbox events permanently failed")
                .register(meterRegistry);
        this.outboxRetried = Counter.builder("amarkatha.outbox.retried")
                .description("Domain outbox transient retries")
                .register(meterRegistry);
    }

    public void notificationCreated() {
        notificationsCreated.increment();
    }

    public void emailQueued() {
        emailQueued.increment();
    }

    public void emailSent() {
        emailSent.increment();
    }

    public void emailFailed() {
        emailFailed.increment();
    }

    public void emailRetried() {
        emailRetried.increment();
    }

    public void emailSkipped() {
        emailSkipped.increment();
    }

    public void outboxProcessed() {
        outboxProcessed.increment();
    }

    public void outboxFailed() {
        outboxFailed.increment();
    }

    public void outboxRetried() {
        outboxRetried.increment();
    }
}
