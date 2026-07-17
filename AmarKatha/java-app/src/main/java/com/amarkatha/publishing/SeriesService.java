package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.SeriesStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeriesService {

    public static final int MAX_ONGOING_SERIES_PER_CREATOR = 5;

    private final SeriesRepository seriesRepository;

    public SeriesService(SeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Transactional(readOnly = true)
    public List<Series> listForCreator(UUID creatorId) {
        return seriesRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId);
    }

    @Transactional(readOnly = true)
    public long countOngoing(UUID creatorId) {
        return seriesRepository.countByCreatorIdAndStatus(creatorId, SeriesStatus.ONGOING);
    }

    @Transactional(readOnly = true)
    public boolean canCreateOngoing(UUID creatorId) {
        return countOngoing(creatorId) < MAX_ONGOING_SERIES_PER_CREATOR;
    }

    @Transactional(readOnly = true)
    public Series requireOwned(UUID seriesId, UUID creatorId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new SeriesAccessException("Series not found"));
        if (!series.getCreatorId().equals(creatorId)) {
            throw new SeriesAccessException("Series not found");
        }
        return series;
    }

    @Transactional
    public Series createSeries(UUID creatorId, String title, String description, String contentLanguage) {
        enforceOngoingCap(creatorId);
        String slug = uniqueSlug(SlugGenerator.fromTitle(title));
        Series series = Series.create(creatorId, slug, title.trim());
        if (description != null && !description.isBlank()) {
            series.setDescription(description.trim());
        }
        if (contentLanguage != null && !contentLanguage.isBlank()) {
            series.setContentLanguage(contentLanguage.trim());
        }
        return seriesRepository.save(series);
    }

    @Transactional
    public Series updateSeries(
            UUID seriesId,
            UUID creatorId,
            String title,
            String description,
            String contentLanguage
    ) {
        Series series = requireOwned(seriesId, creatorId);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        series.setTitle(title.trim());
        series.setDescription(description == null || description.isBlank() ? null : description.trim());
        if (contentLanguage != null && !contentLanguage.isBlank()) {
            series.setContentLanguage(contentLanguage.trim());
        }
        return seriesRepository.save(series);
    }

    @Transactional
    public void markLastPublished(UUID seriesId, UUID creatorId, Instant when) {
        Series series = requireOwned(seriesId, creatorId);
        series.setLastPublishedAt(when);
        seriesRepository.save(series);
    }

    @Transactional(readOnly = true)
    public void enforceOngoingCap(UUID creatorId) {
        long ongoing = countOngoing(creatorId);
        if (ongoing >= MAX_ONGOING_SERIES_PER_CREATOR) {
            throw new OngoingSeriesCapExceededException(MAX_ONGOING_SERIES_PER_CREATOR);
        }
    }

    private String uniqueSlug(String baseSlug) {
        String candidate = baseSlug;
        int suffix = 2;
        while (seriesRepository.existsBySlug(candidate)) {
            candidate = SlugGenerator.withSuffix(baseSlug, suffix++);
        }
        return candidate;
    }
}
