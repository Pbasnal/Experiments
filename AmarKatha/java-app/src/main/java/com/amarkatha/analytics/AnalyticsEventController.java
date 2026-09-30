package com.amarkatha.analytics;

import com.amarkatha.business.ExperienceSourcePolicy;
import com.amarkatha.shared.demo.DemoModeSignals;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reader/v1/events")
public class AnalyticsEventController {

    private final AnalyticsIngestionService analyticsIngestionService;
    private final ExperienceSourcePolicy experienceSourcePolicy;

    public AnalyticsEventController(
            AnalyticsIngestionService analyticsIngestionService,
            ExperienceSourcePolicy experienceSourcePolicy
    ) {
        this.analyticsIngestionService = analyticsIngestionService;
        this.experienceSourcePolicy = experienceSourcePolicy;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void productOrRumEvent(
            @RequestBody ProductEventRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (demo(request)) {
            return;
        }
        analyticsIngestionService.recordClientEvent(body, request, response);
    }

    @PostMapping("/series-view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void seriesView(
            @RequestBody SeriesViewRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (body == null || body.seriesSlug() == null || body.seriesSlug().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "seriesSlug required"
            );
        }
        if (demo(request)) {
            return;
        }
        analyticsIngestionService.recordSeriesView(body.seriesSlug(), body.referrer(), request, response);
    }

    @PostMapping("/chapter-view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void chapterView(
            @RequestBody ChapterViewRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (body == null
                || body.seriesSlug() == null || body.seriesSlug().isBlank()
                || body.chapterSlug() == null || body.chapterSlug().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "seriesSlug and chapterSlug required"
            );
        }
        if (demo(request)) {
            return;
        }
        analyticsIngestionService.recordChapterView(
                body.seriesSlug(),
                body.chapterSlug(),
                body.referrer(),
                request,
                response
        );
    }

    private boolean demo(HttpServletRequest request) {
        return experienceSourcePolicy.demo(DemoModeSignals.admin(request), DemoModeSignals.requested(request));
    }

    public record SeriesViewRequest(String seriesSlug, String referrer) {
    }

    public record ChapterViewRequest(String seriesSlug, String chapterSlug, String referrer) {
    }
}
