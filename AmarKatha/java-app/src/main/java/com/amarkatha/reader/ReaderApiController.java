package com.amarkatha.reader;

import com.amarkatha.reader.dto.ChapterReaderDto;
import com.amarkatha.reader.dto.HomeResponse;
import com.amarkatha.reader.dto.PlatformRouteDto;
import com.amarkatha.reader.dto.SeriesDetailDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reader/v1")
public class ReaderApiController {

    private final ReaderCatalogService readerCatalogService;

    public ReaderApiController(ReaderCatalogService readerCatalogService) {
        this.readerCatalogService = readerCatalogService;
    }

    @GetMapping("/home")
    public HomeResponse home() {
        return new HomeResponse(
                "Publish on your rhythm, share a link, readers know when you're back.",
                readerCatalogService.recentlyUpdated(),
                platformRoutes()
        );
    }

    @GetMapping("/series/{slug}")
    public SeriesDetailDto series(@PathVariable String slug) {
        return readerCatalogService.seriesBySlug(slug);
    }

    @GetMapping("/series/{seriesSlug}/chapters/{chapterSlug}")
    public ChapterReaderDto chapter(
            @PathVariable String seriesSlug,
            @PathVariable String chapterSlug
    ) {
        return readerCatalogService.chapterBySlug(seriesSlug, chapterSlug);
    }

    private static List<PlatformRouteDto> platformRoutes() {
        return List.of(
                new PlatformRouteDto("Home catalog", "/", "Reader", "live", "Chronological series with listed chapters"),
                new PlatformRouteDto("Series hub", "/read/s/{slug}", "Reader", "live", "Schedule strip, chapter list, share link"),
                new PlatformRouteDto("Chapter reader", "/read/s/{slug}/c/{chapter}", "Reader", "live", "Vertical scroll page images"),
                new PlatformRouteDto("Creator home", "/creator", "Creator", "preview", "Next slot, drafts, quick upload"),
                new PlatformRouteDto("Series management", "/creator/series", "Creator", "preview", "List, create, edit series (5 ongoing cap)"),
                new PlatformRouteDto("Series detail", "/creator/series/{id}", "Creator", "preview", "Metadata + chapters"),
                new PlatformRouteDto("Chapter editor", "/creator/series/{id}/chapters/{id}/edit", "Creator", "preview", "Multi-image upload & publish now"),
                new PlatformRouteDto("Sign up / Sign in", "/creator/signup", "Auth", "preview", "Google OAuth; bootstrap admins skip invite"),
                new PlatformRouteDto("Creator login", "/creator/login", "Auth", "preview", "Returning creators and admins"),
                new PlatformRouteDto("Legal / grievance", "/legal/grievance", "Ops", "live", "Terms, privacy, grievance officer")
        );
    }
}
