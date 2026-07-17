package com.amarkatha.analytics;

import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.ChapterState;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalyticsIngestionService {

    private static final String COOKIE_NAME = "reader_id";
    private static final int COOKIE_MAX_AGE = 365 * 24 * 60 * 60;
    private static final Set<String> ALLOWED_REFERRERS = Set.of(
            "share", "homepage", "direct", "external"
    );

    private final AnalyticsEventRepository analyticsEventRepository;
    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;

    public AnalyticsIngestionService(
            AnalyticsEventRepository analyticsEventRepository,
            SeriesRepository seriesRepository,
            ChapterRepository chapterRepository
    ) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.seriesRepository = seriesRepository;
        this.chapterRepository = chapterRepository;
    }

    @Transactional
    public void recordSeriesView(
            String seriesSlug,
            String referrer,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Series series = seriesRepository.findBySlug(seriesSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Series not found"));
        String readerId = ensureReaderId(request, response);
        analyticsEventRepository.save(AnalyticsEvent.create(
                AnalyticsEventType.SERIES_VIEW,
                series.getId(),
                null,
                readerId,
                normalizeReferrer(referrer)
        ));
    }

    @Transactional
    public void recordChapterView(
            String seriesSlug,
            String chapterSlug,
            String referrer,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Series series = seriesRepository.findBySlug(seriesSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Series not found"));
        Chapter chapter = chapterRepository
                .findBySeriesIdAndSlugAndStateAndListedAtIsNotNull(
                        series.getId(),
                        chapterSlug,
                        ChapterState.PUBLISHED
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        String readerId = ensureReaderId(request, response);
        analyticsEventRepository.save(AnalyticsEvent.create(
                AnalyticsEventType.CHAPTER_VIEW,
                series.getId(),
                chapter.getId(),
                readerId,
                normalizeReferrer(referrer)
        ));
    }

    private String ensureReaderId(HttpServletRequest request, HttpServletResponse response) {
        String existing = readCookie(request, COOKIE_NAME);
        if (existing != null && looksLikeUuid(existing)) {
            return existing;
        }
        String created = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(COOKIE_NAME, created);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");
        if (request.isSecure()) {
            cookie.setSecure(true);
        }
        response.addCookie(cookie);
        return created;
    }

    private static String normalizeReferrer(String referrer) {
        if (referrer == null || referrer.isBlank()) {
            return "direct";
        }
        String value = referrer.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_REFERRERS.contains(value) ? value : "external";
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static boolean looksLikeUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
