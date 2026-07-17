package com.amarkatha.publishing;

import com.amarkatha.media.MediaStore;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.SeriesStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SeriesService {

    public static final int MAX_ONGOING_SERIES_PER_CREATOR = 5;

    private static final Set<String> ALLOWED_COVER_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final SeriesRepository seriesRepository;
    private final MediaStore mediaStore;
    private final WebpConversionService webpConversionService;
    private final long maxCoverBytes;

    public SeriesService(
            SeriesRepository seriesRepository,
            MediaStore mediaStore,
            WebpConversionService webpConversionService,
            @Value("${amarkatha.media.max-page-bytes:16777216}") long maxCoverBytes
    ) {
        this.seriesRepository = seriesRepository;
        this.mediaStore = mediaStore;
        this.webpConversionService = webpConversionService;
        this.maxCoverBytes = maxCoverBytes;
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
    public Series uploadCover(UUID seriesId, UUID creatorId, MultipartFile file) {
        Series series = requireOwned(seriesId, creatorId);
        if (file == null || file.isEmpty()) {
            throw new ChapterException("Select a cover image to upload.");
        }
        if (file.getSize() > maxCoverBytes) {
            throw new ChapterException(
                    "Cover must be at most " + (maxCoverBytes / (1024 * 1024)) + " MB."
            );
        }
        String contentType = resolveCoverContentType(file);
        if (!ALLOWED_COVER_TYPES.contains(contentType)) {
            throw new ChapterException("Only PNG, JPG, and WebP covers are allowed.");
        }
        String ext = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        // Wipe prior originals/derivatives so we never reconvert a stale original.jpg
        // and so the public URL changes (immutable browser cache).
        mediaStore.deletePrefix("series/" + seriesId + "/cover/");
        String key = "series/" + seriesId + "/cover/original." + ext;
        try {
            MediaStore.UploadResult uploaded = mediaStore.putOriginal(
                    key,
                    new ByteArrayInputStream(file.getBytes()),
                    contentType
            );
            series.setCoverStorageKey(uploaded.key());
            Series saved = seriesRepository.save(series);
            scheduleCoverWebp(saved.getId());
            return saved;
        } catch (IOException | UncheckedIOException ex) {
            throw new ChapterException("Failed to store cover image.");
        }
    }

    @Transactional
    public Series removeCover(UUID seriesId, UUID creatorId) {
        Series series = requireOwned(seriesId, creatorId);
        mediaStore.deletePrefix("series/" + seriesId + "/cover/");
        series.setCoverStorageKey(null);
        return seriesRepository.save(series);
    }

    private static String resolveCoverContentType(MultipartFile file) {
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (ALLOWED_COVER_TYPES.contains(contentType)) {
            return contentType;
        }
        String name = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return contentType;
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

    private void scheduleCoverWebp(UUID seriesId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    webpConversionService.convertCoverAsync(seriesId);
                }
            });
        } else {
            webpConversionService.convertCoverAsync(seriesId);
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
