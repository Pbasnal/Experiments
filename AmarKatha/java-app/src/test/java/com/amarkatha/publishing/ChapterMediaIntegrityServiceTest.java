package com.amarkatha.publishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.amarkatha.media.MediaStore;
import com.amarkatha.publishing.domain.ChapterPage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChapterMediaIntegrityServiceTest {

    @Mock
    private ChapterPageRepository chapterPageRepository;
    @Mock
    private MediaStore mediaStore;

    private ChapterMediaIntegrityService service;
    private UUID chapterId;

    @BeforeEach
    void setUp() {
        service = new ChapterMediaIntegrityService(chapterPageRepository, mediaStore);
        chapterId = UUID.randomUUID();
    }

    @Test
    void requireIntactFailsWhenNoPages() {
        when(chapterPageRepository.findByChapterIdOrderBySortOrderAsc(chapterId)).thenReturn(List.of());

        ChapterException ex = assertThrows(
                ChapterException.class,
                () -> service.requireIntactForPublish(chapterId)
        );
        assertTrue(ex.getMessage().contains("at least one page"));
    }

    @Test
    void requireIntactFailsWhenFileMissing() {
        ChapterPage page = ChapterPage.create(chapterId, 1, "chapters/x/1.jpg", "1.jpg", 10L, 100, 100);
        when(chapterPageRepository.findByChapterIdOrderBySortOrderAsc(chapterId)).thenReturn(List.of(page));
        when(mediaStore.exists("chapters/x/1.jpg")).thenReturn(false);

        ChapterException ex = assertThrows(
                ChapterException.class,
                () -> service.requireIntactForPublish(chapterId)
        );
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    void intactWhenAllFilesPresent() {
        ChapterPage page = ChapterPage.create(chapterId, 1, "chapters/x/1.jpg", "1.jpg", 10L, 100, 100);
        when(chapterPageRepository.findByChapterIdOrderBySortOrderAsc(chapterId)).thenReturn(List.of(page));
        when(mediaStore.exists("chapters/x/1.jpg")).thenReturn(true);

        assertTrue(service.isChapterMediaIntact(chapterId));
        assertEquals(List.of(), service.findMissingKeys(chapterId));
        service.requireIntactForPublish(chapterId);
    }
}
