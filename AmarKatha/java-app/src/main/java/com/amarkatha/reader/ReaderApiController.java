package com.amarkatha.reader;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.reader.dto.ChapterReaderDto;
import com.amarkatha.reader.dto.HomeResponse;
import com.amarkatha.reader.dto.ReaderFeaturesDto;
import com.amarkatha.reader.dto.SeriesDetailDto;
import com.amarkatha.shared.ReaderFeatureGate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reader/v1")
public class ReaderApiController {

    private final ReaderCatalogService readerCatalogService;
    private final ReaderFeatureGate readerFeatureGate;

    public ReaderApiController(ReaderCatalogService readerCatalogService, ReaderFeatureGate readerFeatureGate) {
        this.readerCatalogService = readerCatalogService;
        this.readerFeatureGate = readerFeatureGate;
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
    public HomeResponse home(Locale locale, @AuthenticationPrincipal AmarKathaPrincipal principal) {
        return readerCatalogService.home(locale, viewerId(principal));
    }

    @GetMapping("/series/{slug}")
    public SeriesDetailDto series(
            @PathVariable String slug,
            @AuthenticationPrincipal AmarKathaPrincipal principal
    ) {
        return readerCatalogService.seriesBySlug(slug, viewerId(principal));
    }

    @GetMapping("/series/{seriesSlug}/chapters/{chapterSlug}")
    public ChapterReaderDto chapter(
            @PathVariable String seriesSlug,
            @PathVariable String chapterSlug
    ) {
        return readerCatalogService.chapterBySlug(seriesSlug, chapterSlug);
    }

    private static UUID viewerId(AmarKathaPrincipal principal) {
        return principal == null ? null : principal.getId();
    }
}
