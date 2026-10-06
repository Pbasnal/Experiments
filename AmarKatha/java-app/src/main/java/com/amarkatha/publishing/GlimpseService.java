package com.amarkatha.publishing;

import com.amarkatha.business.GlimpsePostPolicy;
import com.amarkatha.business.GlimpseTag;
import com.amarkatha.media.MediaStore;
import com.amarkatha.outbox.DomainEventPublisher;
import com.amarkatha.outbox.DomainEventTypes;
import com.amarkatha.publishing.domain.Glimpse;
import com.amarkatha.publishing.domain.GlimpseImage;
import com.amarkatha.publishing.domain.Series;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GlimpseService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    private final SeriesService seriesService;
    private final GlimpseRepository glimpseRepository;
    private final GlimpseImageRepository imageRepository;
    private final MediaStore mediaStore;
    private final DomainEventPublisher domainEventPublisher;
    private final GlimpsePostPolicy glimpsePostPolicy;
    private final long maxImageBytes;

    public GlimpseService(
            SeriesService seriesService,
            GlimpseRepository glimpseRepository,
            GlimpseImageRepository imageRepository,
            MediaStore mediaStore,
            DomainEventPublisher domainEventPublisher,
            GlimpsePostPolicy glimpsePostPolicy,
            @Value("${amarkatha.media.max-page-bytes:16777216}") long maxImageBytes
    ) {
        this.seriesService = seriesService;
        this.glimpseRepository = glimpseRepository;
        this.imageRepository = imageRepository;
        this.mediaStore = mediaStore;
        this.domainEventPublisher = domainEventPublisher;
        this.glimpsePostPolicy = glimpsePostPolicy;
        this.maxImageBytes = maxImageBytes;
    }

    @Transactional(readOnly = true)
    public List<Glimpse> listForSeries(UUID seriesId, UUID creatorId) {
        seriesService.requireOwned(seriesId, creatorId);
        return glimpseRepository.findBySeriesIdOrderByPostedAtAsc(seriesId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<GlimpseImage>> imagesByGlimpse(List<Glimpse> glimpses) {
        Map<UUID, List<GlimpseImage>> images = new LinkedHashMap<>();
        if (glimpses.isEmpty()) {
            return images;
        }
        List<UUID> ids = glimpses.stream().map(Glimpse::getId).toList();
        for (GlimpseImage image : imageRepository.findByGlimpseIdInOrderBySortOrderAsc(ids)) {
            images.computeIfAbsent(image.getGlimpseId(), ignored -> new ArrayList<>()).add(image);
        }
        return images;
    }

    @Transactional
    public Glimpse publish(
            UUID seriesId,
            UUID creatorId,
            String tag,
            List<MultipartFile> files,
            boolean copyrightAck
    ) {
        if (!copyrightAck) {
            throw new GlimpseException("You must confirm copyright ownership before posting.");
        }
        Series series = seriesService.requireOwned(seriesId, creatorId);
        List<MultipartFile> images = nonempty(files);
        GlimpseTag parsed;
        try {
            parsed = glimpsePostPolicy.requireValid(tag, images.size());
        } catch (IllegalArgumentException ex) {
            throw new GlimpseException(ex.getMessage());
        }

        UUID glimpseId = UUID.randomUUID();
        Instant postedAt = Instant.now();
        List<StoredImage> stored = new ArrayList<>();
        try {
            int sortOrder = 1;
            for (MultipartFile file : images) {
                validateFile(file);
                byte[] bytes = file.getBytes();
                ImageMeta meta = readImageMeta(bytes);
                String key = "glimpses/" + glimpseId + "/" + sortOrder + "." + extensionFor(file);
                MediaStore.UploadResult uploaded = mediaStore.putOriginal(
                        key,
                        new ByteArrayInputStream(bytes),
                        file.getContentType()
                );
                stored.add(new StoredImage(sortOrder, uploaded.key(), meta.width(), meta.height()));
                sortOrder++;
            }
            Glimpse glimpse = Glimpse.create(glimpseId, series.getId(), parsed.name(), postedAt);
            glimpseRepository.save(glimpse);
            for (StoredImage image : stored) {
                imageRepository.save(GlimpseImage.create(
                        glimpseId,
                        image.sortOrder(),
                        image.storageKey(),
                        image.width(),
                        image.height()
                ));
            }
            appendPublished(series, glimpse);
            return glimpse;
        } catch (IOException | UncheckedIOException ex) {
            mediaStore.deletePrefix("glimpses/" + glimpseId);
            throw new GlimpseException("Failed to store a glimpse image.");
        } catch (RuntimeException ex) {
            mediaStore.deletePrefix("glimpses/" + glimpseId);
            throw ex;
        }
    }

    @Transactional
    public void delete(UUID seriesId, UUID glimpseId, UUID creatorId) {
        seriesService.requireOwned(seriesId, creatorId);
        Glimpse glimpse = glimpseRepository.findByIdAndSeriesId(glimpseId, seriesId)
                .orElseThrow(() -> new GlimpseException("Glimpse not found."));
        mediaStore.deletePrefix("glimpses/" + glimpse.getId());
        glimpseRepository.delete(glimpse);
    }

    private void appendPublished(Series series, Glimpse glimpse) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("seriesId", series.getId().toString());
        payload.put("glimpseId", glimpse.getId().toString());
        payload.put("seriesSlug", series.getSlug());
        payload.put("seriesTitle", series.getTitle());
        payload.put("tag", glimpse.getTag());
        payload.put("postedAt", glimpse.getPostedAt().toString());
        domainEventPublisher.append(
                DomainEventTypes.GLIMPSE_PUBLISHED,
                "glimpse",
                glimpse.getId(),
                DomainEventTypes.GLIMPSE_PUBLISHED + ":" + glimpse.getId(),
                payload
        );
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > maxImageBytes) {
            throw new GlimpseException(
                    "Each image must be at most " + (maxImageBytes / (1024 * 1024)) + " MB."
            );
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new GlimpseException("Only PNG, JPG, and WebP images are allowed.");
        }
    }

    private static List<MultipartFile> nonempty(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<MultipartFile> images = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                images.add(file);
            }
        }
        return images;
    }

    private static String extensionFor(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private static ImageMeta readImageMeta(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return new ImageMeta(null, null);
            }
            return new ImageMeta(image.getWidth(), image.getHeight());
        } catch (IOException ex) {
            return new ImageMeta(null, null);
        }
    }

    private record StoredImage(int sortOrder, String storageKey, Integer width, Integer height) {
    }

    private record ImageMeta(Integer width, Integer height) {
    }
}
