package com.amarkatha.reader;

import com.amarkatha.reader.dto.ChapterReaderDto;
import com.amarkatha.reader.dto.HomeResponse;
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
                "Publish on your rhythm. Share a link. Readers know when you're back.",
                readerCatalogService.recentlyUpdated(),
                List.of()
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
}
