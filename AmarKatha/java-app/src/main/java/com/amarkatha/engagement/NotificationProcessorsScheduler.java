package com.amarkatha.engagement;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "amarkatha.notifications.processors-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificationProcessorsScheduler {

    private final ChapterPublishedOutboxProcessor outboxProcessor;
    private final NotificationEmailDeliveryProcessor emailDeliveryProcessor;

    public NotificationProcessorsScheduler(
            ChapterPublishedOutboxProcessor outboxProcessor,
            NotificationEmailDeliveryProcessor emailDeliveryProcessor
    ) {
        this.outboxProcessor = outboxProcessor;
        this.emailDeliveryProcessor = emailDeliveryProcessor;
    }

    @Scheduled(fixedDelayString = "${amarkatha.notifications.outbox-poll-interval-ms:5000}")
    public void pollOutbox() {
        outboxProcessor.processPendingBatch();
    }

    @Scheduled(fixedDelayString = "${amarkatha.notifications.email-poll-interval-ms:10000}")
    public void pollEmailDelivery() {
        emailDeliveryProcessor.processPendingBatch();
    }
}
