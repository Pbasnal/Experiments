package com.amarkatha.analytics;

import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.ChapterState;
import com.amarkatha.shared.web.ReaderIdCookie;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalyticsIngestionService {

    private static final Set<String> ALLOWED_REFERRERS = Set.of(
            "share", "homepage", "direct", "external", "app"
    );
    private static final Set<String> ALLOWED_META_KEYS = Set.of(
            "value", "rating", "name", "message", "bulk", "count"
    );
    private static final Set<String> ALLOWED_RATINGS = Set.of(
            "good", "needs-improvement", "poor"
    );
    private static final Pattern EMAIL_LIKE = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern URL_LIKE = Pattern.compile(
            "(?i)(?:https?://|www\\.)\\S+"
    );
    private static final Pattern BEARER_OR_TOKEN = Pattern.compile(
            "(?i)(?:bearer\\s+[A-Za-z0-9._\\-+/=]+|(?:access_token|refresh_token|id_token|api[_-]?key|token|secret|password|authorization)\\s*[:=]\\s*[^\\s\"'&,;]+)"
    );
    private static final Pattern PHONE_LIKE = Pattern.compile(
            "(?<!\\w)(?:\\+?\\d[\\d\\s().-]{7,}\\d)"
    );
    private static final Pattern LONG_IDENTIFIER = Pattern.compile("\\b[A-Za-z0-9_-]{32,}\\b");
    private static final int META_JSON_MAX = 512;
    private static final int EXCEPTION_NAME_MAX = 64;
    private static final int EXCEPTION_MESSAGE_MAX = 200;

    private final AnalyticsEventRepository analyticsEventRepository;
    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsIngestionService(
            AnalyticsEventRepository analyticsEventRepository,
            SeriesRepository seriesRepository,
            ChapterRepository chapterRepository,
            ObjectMapper objectMapper
    ) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.seriesRepository = seriesRepository;
        this.chapterRepository = chapterRepository;
        this.objectMapper = objectMapper;
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
        String readerId = ReaderIdCookie.ensure(request, response);
        save(AnalyticsEventType.SERIES_VIEW, series.getId(), null, readerId, normalizeReferrer(referrer), null);
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
        String readerId = ReaderIdCookie.ensure(request, response);
        save(
                AnalyticsEventType.CHAPTER_VIEW,
                series.getId(),
                chapter.getId(),
                readerId,
                normalizeReferrer(referrer),
                null
        );
    }

    /**
     * Client allowlisted product / RUM events. Server-owned types are rejected here
     * so follow/signup/read are not double-counted from the browser.
     */
    @Transactional
    public void recordClientEvent(
            ProductEventRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (body == null || body.type() == null || body.type().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type required");
        }
        AnalyticsEventType type = parseType(body.type());
        if (!type.clientIngestible()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event type is server-owned; do not send from the browser"
            );
        }
        ResolvedIds ids = resolveOptionalSeriesChapter(body.seriesSlug(), body.chapterSlug());
        String metaJson = validateAndSerializeMeta(type, body.meta());
        String readerId = ReaderIdCookie.ensure(request, response);
        save(type, ids.seriesId(), ids.chapterId(), readerId, normalizeReferrer(body.referrer()), metaJson);
    }

    @Transactional
    public void recordServerEvent(
            AnalyticsEventType type,
            UUID seriesId,
            UUID chapterId,
            String readerId,
            String referrer,
            String metaJson
    ) {
        if (type == null || type.clientIngestible()) {
            throw new IllegalArgumentException("Server event type required");
        }
        String safeReaderId = (readerId == null || readerId.isBlank())
                ? UUID.randomUUID().toString()
                : readerId;
        save(type, seriesId, chapterId, safeReaderId, normalizeReferrer(referrer), metaJson);
    }

    @Transactional
    public void recordServerEvent(
            AnalyticsEventType type,
            UUID seriesId,
            UUID chapterId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String readerId = ReaderIdCookie.ensure(request, response);
        recordServerEvent(type, seriesId, chapterId, readerId, "app", null);
    }

    private void save(
            AnalyticsEventType type,
            UUID seriesId,
            UUID chapterId,
            String readerId,
            String referrer,
            String metaJson
    ) {
        analyticsEventRepository.save(AnalyticsEvent.create(
                type,
                seriesId,
                chapterId,
                readerId,
                referrer,
                metaJson
        ));
    }

    static AnalyticsEventType parseType(String raw) {
        try {
            return AnalyticsEventType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown event type");
        }
    }

    String validateAndSerializeMeta(AnalyticsEventType type, Map<String, Object> meta) {
        Map<String, Object> cleaned = sanitizeMeta(meta);
        if (type.requiresMetaValue()) {
            Object value = cleaned.get("value");
            if (!(value instanceof Number)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meta.value number required");
            }
        }
        if (type.requiresExceptionName()) {
            Object name = cleaned.get("name");
            if (!(name instanceof String s) || s.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meta.name required");
            }
        }
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(cleaned);
            if (json.length() > META_JSON_MAX) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meta too large");
            }
            return json;
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid meta");
        }
    }

    static Map<String, Object> sanitizeMeta(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : meta.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (!ALLOWED_META_KEYS.contains(key) || entry.getValue() == null) {
                continue;
            }
            Object value = entry.getValue();
            switch (key) {
                case "value" -> {
                    if (value instanceof Number number) {
                        cleaned.put(key, number.doubleValue());
                    }
                }
                case "count" -> {
                    if (value instanceof Number number) {
                        cleaned.put(key, number.intValue());
                    }
                }
                case "bulk" -> {
                    if (value instanceof Boolean bool) {
                        cleaned.put(key, bool);
                    }
                }
                case "rating" -> {
                    String rating = value.toString().trim().toLowerCase(Locale.ROOT);
                    if (ALLOWED_RATINGS.contains(rating)) {
                        cleaned.put(key, rating);
                    }
                }
                case "name" -> {
                    String name = stripPii(value.toString()).trim();
                    if (!name.isEmpty()) {
                        cleaned.put(key, truncate(name, EXCEPTION_NAME_MAX));
                    }
                }
                case "message" -> {
                    String message = stripPii(value.toString()).trim();
                    if (!message.isEmpty()) {
                        cleaned.put(key, truncate(message, EXCEPTION_MESSAGE_MAX));
                    }
                }
                default -> {
                }
            }
        }
        return cleaned;
    }

    private ResolvedIds resolveOptionalSeriesChapter(String seriesSlug, String chapterSlug) {
        if (seriesSlug == null || seriesSlug.isBlank()) {
            if (chapterSlug != null && !chapterSlug.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seriesSlug required with chapterSlug");
            }
            return new ResolvedIds(null, null);
        }
        Series series = seriesRepository.findBySlug(seriesSlug.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Series not found"));
        if (chapterSlug == null || chapterSlug.isBlank()) {
            return new ResolvedIds(series.getId(), null);
        }
        Chapter chapter = chapterRepository
                .findBySeriesIdAndSlugAndStateAndListedAtIsNotNull(
                        series.getId(),
                        chapterSlug.trim(),
                        ChapterState.PUBLISHED
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        return new ResolvedIds(series.getId(), chapter.getId());
    }

    static String normalizeReferrer(String referrer) {
        if (referrer == null || referrer.isBlank()) {
            return "direct";
        }
        String value = referrer.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_REFERRERS.contains(value) ? value : "external";
    }

    static String stripPii(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        String value = EMAIL_LIKE.matcher(raw).replaceAll("[redacted]");
        value = URL_LIKE.matcher(value).replaceAll("[redacted-url]");
        value = BEARER_OR_TOKEN.matcher(value).replaceAll("[redacted-secret]");
        value = PHONE_LIKE.matcher(value).replaceAll("[redacted-phone]");
        value = LONG_IDENTIFIER.matcher(value).replaceAll("[redacted-id]");
        return value;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record ResolvedIds(UUID seriesId, UUID chapterId) {
    }
}
