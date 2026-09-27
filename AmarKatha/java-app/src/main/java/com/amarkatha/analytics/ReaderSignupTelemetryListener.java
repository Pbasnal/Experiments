package com.amarkatha.analytics;

import com.amarkatha.shared.events.ReaderSignedUpTelemetry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReaderSignupTelemetryListener {

    private final AnalyticsIngestionService analyticsIngestionService;

    public ReaderSignupTelemetryListener(AnalyticsIngestionService analyticsIngestionService) {
        this.analyticsIngestionService = analyticsIngestionService;
    }

    @EventListener
    @Transactional
    public void onReaderSignedUp(ReaderSignedUpTelemetry event) {
        if (event == null) {
            return;
        }
        analyticsIngestionService.recordServerEvent(
                AnalyticsEventType.SIGNUP_SUCCESS,
                null,
                null,
                event.anonymousReaderId(),
                "app",
                null
        );
    }
}
