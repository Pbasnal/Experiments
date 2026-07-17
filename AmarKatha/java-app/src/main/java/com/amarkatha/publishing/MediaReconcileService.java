package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.shared.domain.ChapterState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Syncs catalog visibility with media store: delist broken chapters, relist repaired ones.
 */
@Service
public class MediaReconcileService {

    private static final Logger log = LoggerFactory.getLogger(MediaReconcileService.class);

    private final ChapterRepository chapterRepository;
    private final ChapterMediaIntegrityService integrityService;

    public MediaReconcileService(
            ChapterRepository chapterRepository,
            ChapterMediaIntegrityService integrityService
    ) {
        this.chapterRepository = chapterRepository;
        this.integrityService = integrityService;
    }

    @Transactional
    public ReconcileReport reconcile() {
        List<Chapter> published = chapterRepository.findByState(ChapterState.PUBLISHED);
        List<BrokenChapter> delisted = new ArrayList<>();
        List<UUID> relisted = new ArrayList<>();
        int intactListed = 0;

        for (Chapter chapter : published) {
            boolean intact = integrityService.isChapterMediaIntact(chapter.getId());
            boolean listed = chapter.getListedAt() != null;

            if (listed && !intact) {
                List<String> missing = integrityService.findMissingKeys(chapter.getId());
                chapter.delist();
                chapterRepository.save(chapter);
                delisted.add(new BrokenChapter(chapter.getId(), chapter.getSlug(), missing));
                log.warn(
                        "Delisted chapter id={} slug={} missingMedia={}",
                        chapter.getId(),
                        chapter.getSlug(),
                        missing
                );
            } else if (!listed && intact) {
                Instant when = chapter.getPublishedAt() != null ? chapter.getPublishedAt() : Instant.now();
                chapter.relist(when);
                chapterRepository.save(chapter);
                relisted.add(chapter.getId());
                log.info("Relisted chapter id={} slug={} after media restored", chapter.getId(), chapter.getSlug());
            } else if (listed) {
                intactListed++;
            }
        }

        ReconcileReport report = new ReconcileReport(
                published.size(),
                intactListed,
                delisted,
                relisted,
                Instant.now()
        );
        log.info(
                "Media reconcile done: published={} intactListed={} delisted={} relisted={}",
                report.publishedCount(),
                report.intactListedCount(),
                report.delisted().size(),
                report.relisted().size()
        );
        return report;
    }

    @Transactional(readOnly = true)
    public int countBrokenListedChapters() {
        return (int) chapterRepository.findByState(ChapterState.PUBLISHED).stream()
                .filter(c -> c.getListedAt() != null)
                .filter(c -> !integrityService.isChapterMediaIntact(c.getId()))
                .count();
    }

    public record BrokenChapter(UUID chapterId, String slug, List<String> missingKeys) {
    }

    public record ReconcileReport(
            int publishedCount,
            int intactListedCount,
            List<BrokenChapter> delisted,
            List<UUID> relisted,
            Instant ranAt
    ) {
    }
}
