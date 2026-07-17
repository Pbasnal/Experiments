package com.amarkatha.publishing;

import com.amarkatha.media.ImageDerivativeEncoder;
import com.amarkatha.media.MediaStore;
import com.amarkatha.publishing.domain.ChapterPage;
import com.amarkatha.publishing.domain.Series;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebpConversionService {

    private static final Logger log = LoggerFactory.getLogger(WebpConversionService.class);
    private static final int READER_MAX_WIDTH = 800;
    private static final float READER_QUALITY = 0.82f;
    private static final int COVER_MAX_WIDTH = 800;
    private static final float COVER_QUALITY = 0.85f;

    private final ChapterPageRepository chapterPageRepository;
    private final SeriesRepository seriesRepository;
    private final MediaStore mediaStore;
    private final ImageDerivativeEncoder encoder;

    public WebpConversionService(
            ChapterPageRepository chapterPageRepository,
            SeriesRepository seriesRepository,
            MediaStore mediaStore,
            ImageDerivativeEncoder encoder
    ) {
        this.chapterPageRepository = chapterPageRepository;
        this.seriesRepository = seriesRepository;
        this.mediaStore = mediaStore;
        this.encoder = encoder;
    }

    @Async("webpExecutor")
    @Transactional
    public void convertPageAsync(UUID pageId) {
        ChapterPage page = chapterPageRepository.findById(pageId).orElse(null);
        if (page == null) {
            log.warn("WebP skip: page {} not found", pageId);
            return;
        }
        if (page.getWebpStorageKey() != null && mediaStore.exists(page.getWebpStorageKey())) {
            return;
        }
        String originalKey = page.getOriginalStorageKey();
        if (originalKey == null || !mediaStore.exists(originalKey)) {
            log.warn("WebP skip: original missing for page {}", pageId);
            return;
        }
        try {
            byte[] original = Files.readAllBytes(mediaStore.resolvePath(originalKey));
            ImageDerivativeEncoder.Derivative derivative =
                    encoder.toReaderDerivative(original, READER_MAX_WIDTH, READER_QUALITY);
            String derivativeKey = "chapters/" + page.getChapterId() + "/pages/"
                    + page.getSortOrder() + "-reader." + derivative.extension();
            MediaStore.UploadResult uploaded = mediaStore.putDerivative(
                    derivativeKey,
                    new ByteArrayInputStream(derivative.bytes()),
                    derivative.contentType()
            );
            page.applyWebp(uploaded.key(), uploaded.bytes());
            chapterPageRepository.save(page);
            log.info("Reader derivative ready page={} key={}", pageId, uploaded.key());
        } catch (IOException | RuntimeException ex) {
            log.warn("Reader derivative failed for page {}: {}", pageId, ex.getMessage());
        }
    }

    @Async("webpExecutor")
    @Transactional
    public void convertCoverAsync(UUID seriesId) {
        Series series = seriesRepository.findById(seriesId).orElse(null);
        if (series == null) {
            return;
        }
        String originalKey = findCoverOriginalKey(seriesId);
        String sourceKey = originalKey != null ? originalKey : series.getCoverStorageKey();
        if (sourceKey == null || sourceKey.isBlank() || !mediaStore.exists(sourceKey)) {
            return;
        }
        try {
            byte[] original = Files.readAllBytes(mediaStore.resolvePath(sourceKey));
            ImageDerivativeEncoder.Derivative derivative =
                    encoder.toReaderDerivative(original, COVER_MAX_WIDTH, COVER_QUALITY);
            // Unique key so browsers don't keep an immutable cached cover forever.
            String derivativeKey = "series/" + seriesId + "/cover/cover-800-"
                    + System.currentTimeMillis() + "." + derivative.extension();
            MediaStore.UploadResult uploaded = mediaStore.putDerivative(
                    derivativeKey,
                    new ByteArrayInputStream(derivative.bytes()),
                    derivative.contentType()
            );
            // Drop previous derivatives/originals except the source original we just used.
            deleteStaleCoverFiles(seriesId, sourceKey, uploaded.key());
            series.setCoverStorageKey(uploaded.key());
            seriesRepository.save(series);
            log.info("Cover derivative ready series={} key={}", seriesId, uploaded.key());
        } catch (IOException | RuntimeException ex) {
            log.warn("Cover derivative failed for series {}: {}", seriesId, ex.getMessage());
        }
    }

    private String findCoverOriginalKey(UUID seriesId) {
        for (String ext : new String[]{"jpg", "jpeg", "png", "webp"}) {
            String key = "series/" + seriesId + "/cover/original." + ext;
            if (mediaStore.exists(key)) {
                return key;
            }
        }
        return null;
    }

    private void deleteStaleCoverFiles(UUID seriesId, String keepOriginal, String keepDerivative) {
        for (String ext : new String[]{"jpg", "jpeg", "png", "webp"}) {
            String key = "series/" + seriesId + "/cover/original." + ext;
            if (!key.equals(keepOriginal) && mediaStore.exists(key)) {
                mediaStore.delete(key);
            }
        }
        Path coverDir = mediaStore.resolvePath("series/" + seriesId + "/cover/original.jpg").getParent();
        if (coverDir == null || !Files.isDirectory(coverDir)) {
            return;
        }
        try (var stream = Files.list(coverDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("cover-800");
                    })
                    .forEach(path -> {
                        String key = "series/" + seriesId + "/cover/" + path.getFileName();
                        if (!key.equals(keepDerivative)) {
                            mediaStore.delete(key);
                        }
                    });
        } catch (IOException ex) {
            log.warn("Could not clean stale covers for series {}: {}", seriesId, ex.getMessage());
        }
    }
}
