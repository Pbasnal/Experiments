package com.amarkatha.reader;

import com.amarkatha.bootstrap.AbsoluteUrlBuilder;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.SeriesScheduleService;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.scheduling.ScheduleStripView;
import com.amarkatha.shared.domain.ChapterState;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Serves Open Graph HTML for crawlers (WhatsApp, Facebook, Twitter). Humans get the React SPA.
 */
@Controller
public class OpenGraphController {

    private static final Set<String> CRAWLER_TOKENS = Set.of(
            "facebookexternalhit",
            "facebot",
            "twitterbot",
            "whatsapp",
            "linkedinbot",
            "slackbot",
            "discordbot",
            "telegrambot",
            "embedly",
            "quora link preview",
            "showyoubot",
            "outbrain",
            "pinterest",
            "vkshare",
            "w3c_validator",
            "redditbot",
            "applebot",
            "bingpreview"
    );

    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;
    private final SeriesScheduleService seriesScheduleService;
    private final AbsoluteUrlBuilder absoluteUrlBuilder;

    public OpenGraphController(
            SeriesRepository seriesRepository,
            ChapterRepository chapterRepository,
            SeriesScheduleService seriesScheduleService,
            AbsoluteUrlBuilder absoluteUrlBuilder
    ) {
        this.seriesRepository = seriesRepository;
        this.chapterRepository = chapterRepository;
        this.seriesScheduleService = seriesScheduleService;
        this.absoluteUrlBuilder = absoluteUrlBuilder;
    }

    @GetMapping("/read/s/{slug}")
    public String seriesOg(
            @PathVariable String slug,
            HttpServletRequest request,
            Model model
    ) {
        if (!isCrawler(request)) {
            return "forward:/index.html";
        }
        Series series = seriesRepository.findBySlug(slug).orElse(null);
        if (series == null) {
            return "forward:/index.html";
        }
        ScheduleStripView strip = seriesScheduleService.stripFor(series);
        String description = series.getDescription();
        if (description == null || description.isBlank()) {
            description = strip.scheduleLabel();
        }
        String path = "/read/s/" + series.getSlug();
        model.addAttribute("ogTitle", series.getTitle() + " · AmarKatha");
        model.addAttribute("ogDescription", truncate(description, 200));
        model.addAttribute("ogUrl", absoluteUrlBuilder.absolute(request, path));
        model.addAttribute("ogImage", coverImageUrl(request, series));
        model.addAttribute("ogType", "website");
        model.addAttribute("canonicalPath", path);
        return "og/share";
    }

    @GetMapping("/read/s/{seriesSlug}/c/{chapterSlug}")
    public String chapterOg(
            @PathVariable String seriesSlug,
            @PathVariable String chapterSlug,
            HttpServletRequest request,
            Model model
    ) {
        if (!isCrawler(request)) {
            return "forward:/index.html";
        }
        Series series = seriesRepository.findBySlug(seriesSlug).orElse(null);
        if (series == null) {
            return "forward:/index.html";
        }
        Chapter chapter = chapterRepository
                .findBySeriesIdAndSlugAndStateAndListedAtIsNotNull(
                        series.getId(),
                        chapterSlug,
                        ChapterState.PUBLISHED
                )
                .orElse(null);
        if (chapter == null) {
            return "forward:/index.html";
        }
        String path = "/read/s/" + series.getSlug() + "/c/" + chapter.getSlug();
        model.addAttribute("ogTitle", series.getTitle() + " — " + chapter.getTitle());
        model.addAttribute(
                "ogDescription",
                truncate(
                        series.getDescription() == null || series.getDescription().isBlank()
                                ? chapter.getTitle()
                                : series.getDescription(),
                        200
                )
        );
        model.addAttribute("ogUrl", absoluteUrlBuilder.absolute(request, path));
        model.addAttribute("ogImage", coverImageUrl(request, series));
        model.addAttribute("ogType", "article");
        model.addAttribute("canonicalPath", path);
        return "og/share";
    }

    static boolean isCrawler(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return false;
        }
        String lower = ua.toLowerCase(Locale.ROOT);
        for (String token : CRAWLER_TOKENS) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String coverImageUrl(HttpServletRequest request, Series series) {
        if (series.getCoverStorageKey() == null || series.getCoverStorageKey().isBlank()) {
            return null;
        }
        return absoluteUrlBuilder.absolute(
                request,
                "/media/" + series.getCoverStorageKey() + "?v=" + series.getVersion()
        );
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max - 1) + "…";
    }
}
