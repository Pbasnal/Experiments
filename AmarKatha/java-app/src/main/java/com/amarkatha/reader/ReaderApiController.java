package com.amarkatha.reader;

import com.amarkatha.business.ExperienceSourcePolicy;
import com.amarkatha.reader.dto.ChapterReaderDto;
import com.amarkatha.reader.dto.HomeResponse;
import com.amarkatha.reader.dto.ReaderFeaturesDto;
import com.amarkatha.reader.dto.SeriesDetailDto;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.demo.DemoModeSignals;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reader/v1")
public class ReaderApiController {

    private final ReaderCatalogService readerCatalogService;
    private final DemoCatalogService demoCatalogService;
    private final ReaderFeatureGate readerFeatureGate;
    private final ExperienceSourcePolicy experienceSourcePolicy;

    public ReaderApiController(
            ReaderCatalogService readerCatalogService,
            DemoCatalogService demoCatalogService,
            ReaderFeatureGate readerFeatureGate,
            ExperienceSourcePolicy experienceSourcePolicy
    ) {
        this.readerCatalogService = readerCatalogService;
        this.demoCatalogService = demoCatalogService;
        this.readerFeatureGate = readerFeatureGate;
        this.experienceSourcePolicy = experienceSourcePolicy;
    }

    @GetMapping("/features")
    public ReaderFeaturesDto features() {
        var props = readerFeatureGate.properties();
        return new ReaderFeaturesDto(
                props.landingDiscovery(),
                props.profileProgress(),
                props.follows(),
                props.inAppNotifications(),
                props.emailNotifications()
        );
    }

    @GetMapping("/home")
    public HomeResponse home(Locale locale, HttpServletRequest request) {
        if (demo(request)) {
            return demoCatalogService.home(locale);
        }
        return readerCatalogService.home(locale, viewerId(request));
    }

    @GetMapping("/series/{slug}")
    public SeriesDetailDto series(@PathVariable String slug, HttpServletRequest request) {
        if (demo(request)) {
            return demoCatalogService.series(slug);
        }
        return readerCatalogService.seriesBySlug(slug, viewerId(request));
    }

    @GetMapping("/series/{seriesSlug}/chapters/{chapterSlug}")
    public ChapterReaderDto chapter(
            @PathVariable String seriesSlug,
            @PathVariable String chapterSlug,
            HttpServletRequest request
    ) {
        if (demo(request)) {
            return demoCatalogService.chapter(seriesSlug, chapterSlug);
        }
        return readerCatalogService.chapterBySlug(seriesSlug, chapterSlug);
    }

    private boolean demo(HttpServletRequest request) {
        return experienceSourcePolicy.demo(DemoModeSignals.admin(request), DemoModeSignals.requested(request));
    }

    private static UUID viewerId(HttpServletRequest request) {
        Object raw = request.getAttribute(DemoModeSignals.VIEWER_ID);
        return raw instanceof UUID id ? id : null;
    }
}
