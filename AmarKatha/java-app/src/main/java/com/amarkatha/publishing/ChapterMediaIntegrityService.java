package com.amarkatha.publishing;

import com.amarkatha.media.MediaStore;
import com.amarkatha.publishing.domain.ChapterPage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures chapter_page rows and media store keys stay consistent for readers.
 */
@Service
public class ChapterMediaIntegrityService {

    private final ChapterPageRepository chapterPageRepository;
    private final MediaStore mediaStore;

    public ChapterMediaIntegrityService(
            ChapterPageRepository chapterPageRepository,
            MediaStore mediaStore
    ) {
        this.chapterPageRepository = chapterPageRepository;
        this.mediaStore = mediaStore;
    }

    @Transactional(readOnly = true)
    public boolean isChapterMediaIntact(UUID chapterId) {
        return findMissingKeys(chapterId).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<String> findMissingKeys(UUID chapterId) {
        List<ChapterPage> pages = chapterPageRepository.findByChapterIdOrderBySortOrderAsc(chapterId);
        if (pages.isEmpty()) {
            return List.of("(no pages)");
        }
        List<String> missing = new ArrayList<>();
        for (ChapterPage page : pages) {
            String key = effectiveStorageKey(page);
            if (key == null || key.isBlank() || !mediaStore.exists(key)) {
                missing.add(key == null || key.isBlank()
                        ? "page#" + page.getSortOrder() + "(empty key)"
                        : key);
            }
        }
        return missing;
    }

    @Transactional(readOnly = true)
    public void requireIntactForPublish(UUID chapterId) {
        List<String> missing = findMissingKeys(chapterId);
        if (missing.isEmpty()) {
            return;
        }
        if (missing.size() == 1 && "(no pages)".equals(missing.get(0))) {
            throw new ChapterException("Add at least one page before publishing.");
        }
        throw new ChapterException(
                "Cannot publish: " + missing.size()
                        + " page file(s) missing from media storage. Re-upload pages and try again."
        );
    }

    public static String effectiveStorageKey(ChapterPage page) {
        if (page.getWebpStorageKey() != null && !page.getWebpStorageKey().isBlank()) {
            return page.getWebpStorageKey();
        }
        return page.getOriginalStorageKey();
    }
}
