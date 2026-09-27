package com.amarkatha.admin;

import com.amarkatha.publishing.SeriesAccessException;
import com.amarkatha.publishing.SeriesService;
import com.amarkatha.publishing.domain.Series;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin write path for catalog signals the reader home can display.
 * Omitted JSON fields are left unchanged. Zero rating or reader count hides that UI.
 */
@RestController
@RequestMapping("/api/admin/v1/series")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSeriesSignalsController {

    private final SeriesService seriesService;

    public AdminSeriesSignalsController(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    @PutMapping("/{slug}/signals")
    public SeriesSignalsResponse update(
            @PathVariable String slug,
            @RequestBody SeriesSignalsRequest body
    ) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        try {
            Series series = seriesService.updateSignals(
                    slug,
                    body.rating(),
                    body.readerCount(),
                    body.editorsPick()
            );
            return SeriesSignalsResponse.from(series);
        } catch (SeriesAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public record SeriesSignalsRequest(Double rating, Integer readerCount, Boolean editorsPick) {
    }

    public record SeriesSignalsResponse(String slug, double rating, int readerCount, boolean editorsPick) {
        static SeriesSignalsResponse from(Series series) {
            return new SeriesSignalsResponse(
                    series.getSlug(),
                    series.getRating(),
                    series.getReaderCount(),
                    series.isEditorsPick()
            );
        }
    }
}
