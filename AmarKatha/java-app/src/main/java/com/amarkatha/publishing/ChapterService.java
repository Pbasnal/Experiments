package com.amarkatha.publishing;

import com.amarkatha.media.MediaStore;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.ChapterPage;
import com.amarkatha.shared.domain.ChapterState;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChapterService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final SeriesService seriesService;
    private final SeriesScheduleService seriesScheduleService;
    private final MediaStore mediaStore;
    private final ChapterMediaIntegrityService mediaIntegrityService;
    private final WebpConversionService webpConversionService;
    private final int maxPagesPerChapter;
    private final long maxPageBytes;

    public ChapterService(
            ChapterRepository chapterRepository,
            ChapterPageRepository chapterPageRepository,
            SeriesService seriesService,
            SeriesScheduleService seriesScheduleService,
            MediaStore mediaStore,
            ChapterMediaIntegrityService mediaIntegrityService,
            WebpConversionService webpConversionService,
            @Value("${amarkatha.media.max-pages-per-chapter:40}") int maxPagesPerChapter,
            @Value("${amarkatha.media.max-page-bytes:16777216}") long maxPageBytes
    ) {
        this.chapterRepository = chapterRepository;
        this.chapterPageRepository = chapterPageRepository;
        this.seriesService = seriesService;
        this.seriesScheduleService = seriesScheduleService;
        this.mediaStore = mediaStore;
        this.mediaIntegrityService = mediaIntegrityService;
        this.webpConversionService = webpConversionService;
        this.maxPagesPerChapter = maxPagesPerChapter;
        this.maxPageBytes = maxPageBytes;
    }

    @Transactional(readOnly = true)
    public List<Chapter> listForSeries(UUID seriesId, UUID creatorId) {
        seriesService.requireOwned(seriesId, creatorId);
        return chapterRepository.findBySeriesIdOrderByChapterNumberAsc(seriesId);
    }

    @Transactional(readOnly = true)
    public Chapter requireOwnedChapter(UUID seriesId, UUID chapterId, UUID creatorId) {
        seriesService.requireOwned(seriesId, creatorId);
        return chapterRepository.findBySeriesIdAndId(seriesId, chapterId)
                .orElseThrow(() -> new SeriesAccessException("Chapter not found"));
    }

    @Transactional(readOnly = true)
    public List<ChapterPage> listPages(UUID chapterId) {
        return chapterPageRepository.findByChapterIdOrderBySortOrderAsc(chapterId);
    }

    @Transactional
    public Chapter createDraft(UUID seriesId, UUID creatorId, String title) {
        seriesService.requireOwned(seriesId, creatorId);
        double nextNumber = nextChapterNumber(seriesId);
        String slug = uniqueSlug(seriesId, slugFromNumber(nextNumber));
        String resolvedTitle = (title == null || title.isBlank())
                ? "Chapter " + formatNumber(nextNumber)
                : title.trim();
        Chapter chapter = Chapter.createDraft(seriesId, nextNumber, slug, resolvedTitle);
        return chapterRepository.save(chapter);
    }

    @Transactional
    public Chapter updateDraft(UUID seriesId, UUID chapterId, UUID creatorId, String title) {
        Chapter chapter = requireOwnedChapter(seriesId, chapterId, creatorId);
        requireDraft(chapter);
        if (title != null && !title.isBlank()) {
            chapter.setTitle(title.trim());
        }
        return chapterRepository.save(chapter);
    }

    @Transactional
    public List<ChapterPage> addPages(
            UUID seriesId,
            UUID chapterId,
            UUID creatorId,
            List<MultipartFile> files
    ) {
        Chapter chapter = requireOwnedChapter(seriesId, chapterId, creatorId);
        requireDraft(chapter);
        if (files == null || files.isEmpty()) {
            throw new ChapterException("Select at least one image to upload.");
        }

        long existing = chapterPageRepository.countByChapterId(chapterId);
        int incoming = (int) files.stream().filter(f -> f != null && !f.isEmpty()).count();
        if (existing + incoming > maxPagesPerChapter) {
            throw new ChapterException(
                    "A chapter can have at most " + maxPagesPerChapter + " pages (currently "
                            + existing + ")."
            );
        }

        Integer maxOrder = chapterPageRepository.findMaxSortOrder(chapterId);
        int nextOrder = maxOrder == null ? 0 : maxOrder;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            validateFile(file);
            nextOrder += 1;
            String ext = extensionFor(file);
            String key = "chapters/" + chapterId + "/pages/" + nextOrder + "-original." + ext;
            try {
                byte[] bytes = file.getBytes();
                ImageMeta meta = readImageMeta(bytes);
                MediaStore.UploadResult uploaded = mediaStore.putOriginal(
                        key,
                        new ByteArrayInputStream(bytes),
                        file.getContentType()
                );
                ChapterPage page = ChapterPage.create(
                        chapterId,
                        nextOrder,
                        uploaded.key(),
                        uploaded.bytes(),
                        meta.width(),
                        meta.height()
                );
                ChapterPage saved = chapterPageRepository.save(page);
                scheduleWebp(saved.getId());
            } catch (IOException | UncheckedIOException ex) {
                throw new ChapterException(
                        "Failed to store page (check media folder permissions): "
                                + file.getOriginalFilename()
                );
            }
        }
        return listPages(chapterId);
    }

    @Transactional
    public Chapter publishNow(UUID seriesId, UUID chapterId, UUID creatorId, boolean copyrightAck) {
        if (!copyrightAck) {
            throw new ChapterException("You must confirm copyright ownership before publishing.");
        }
        Chapter chapter = requireOwnedChapter(seriesId, chapterId, creatorId);
        requireDraft(chapter);
        mediaIntegrityService.requireIntactForPublish(chapterId);
        Instant now = Instant.now();
        chapter.publishNow(now, now);
        chapterRepository.save(chapter);
        seriesScheduleService.onChapterPublished(seriesId, creatorId, now);
        return chapter;
    }

    public int getMaxPagesPerChapter() {
        return maxPagesPerChapter;
    }

    public long getMaxPageBytes() {
        return maxPageBytes;
    }

    private void scheduleWebp(UUID pageId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    webpConversionService.convertPageAsync(pageId);
                }
            });
        } else {
            webpConversionService.convertPageAsync(pageId);
        }
    }

    private double nextChapterNumber(UUID seriesId) {
        Double max = chapterRepository.findMaxChapterNumber(seriesId);
        double current = max == null ? 0d : max;
        return current + 1d;
    }

    private void requireDraft(Chapter chapter) {
        if (chapter.getState() != ChapterState.DRAFT) {
            throw new ChapterException("Only draft chapters can be edited.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > maxPageBytes) {
            throw new ChapterException(
                    "Each page must be at most " + (maxPageBytes / (1024 * 1024)) + " MB."
            );
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ChapterException("Only PNG, JPG, and WebP images are allowed.");
        }
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

    private String uniqueSlug(UUID seriesId, String baseSlug) {
        String candidate = baseSlug;
        int suffix = 2;
        while (chapterRepository.existsBySeriesIdAndSlug(seriesId, candidate)) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    static String slugFromNumber(double number) {
        if (Math.rint(number) == number) {
            return "chapter-" + (long) number;
        }
        String raw = Double.toString(number).replace('.', '-');
        return "chapter-" + raw;
    }

    private static String formatNumber(double number) {
        if (Math.rint(number) == number) {
            return Long.toString((long) number);
        }
        return Double.toString(number);
    }

    private record ImageMeta(Integer width, Integer height) {
    }
}
