package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.SeriesStatus;
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

    @Transactional
    public Series createSeries(UUID creatorId, String title, String contentLanguage) {
        enforceOngoingCap(creatorId);
        String slug = uniqueSlug(SlugGenerator.fromTitle(title));
        Series series = Series.create(creatorId, slug, title.trim());
        if (contentLanguage != null && !contentLanguage.isBlank()) {
            series.setContentLanguage(contentLanguage);
        }
        return seriesRepository.save(series);
    }

    @Transactional(readOnly = true)
    public void enforceOngoingCap(UUID creatorId) {
        long ongoing = seriesRepository.countByCreatorIdAndStatus(creatorId, SeriesStatus.ONGOING);
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
