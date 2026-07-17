package com.amarkatha.analytics;

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

    public AnalyticsEventController(AnalyticsIngestionService analyticsIngestionService) {
        this.analyticsIngestionService = analyticsIngestionService;
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
        analyticsIngestionService.recordChapterView(
                body.seriesSlug(),
                body.chapterSlug(),
                body.referrer(),
                request,
                response
        );
    }

    public record SeriesViewRequest(String seriesSlug, String referrer) {
    }

    public record ChapterViewRequest(String seriesSlug, String chapterSlug, String referrer) {
    }
}
