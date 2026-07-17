package com.amarkatha.publishing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "amarkatha.media.reconcile-schedule-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MediaReconcileScheduler {

    private final MediaReconcileService mediaReconcileService;

    public MediaReconcileScheduler(MediaReconcileService mediaReconcileService) {
        this.mediaReconcileService = mediaReconcileService;
    }

    @Scheduled(fixedDelayString = "${amarkatha.media.reconcile-interval-ms:3600000}")
    public void scheduledReconcile() {
        mediaReconcileService.reconcile();
    }
}
