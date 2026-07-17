package com.amarkatha.publishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.media.MediaStore;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.ChapterState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ChapterPageRepository chapterPageRepository;
    @Mock
    private SeriesService seriesService;
    @Mock
    private SeriesScheduleService seriesScheduleService;
    @Mock
    private MediaStore mediaStore;
    @Mock
    private ChapterMediaIntegrityService mediaIntegrityService;

    private ChapterService chapterService;
    private UUID creatorId;
    private UUID seriesId;

    @BeforeEach
    void setUp() {
        chapterService = new ChapterService(
                chapterRepository,
                chapterPageRepository,
                seriesService,
                seriesScheduleService,
                mediaStore,
                mediaIntegrityService,
                40,
                16 * 1024 * 1024
        );
        creatorId = UUID.randomUUID();
        seriesId = UUID.randomUUID();
    }

    @Test
    void createDraftAssignsNextNumber() {
        when(seriesService.requireOwned(seriesId, creatorId))
                .thenReturn(Series.create(creatorId, "slug", "Title"));
        when(chapterRepository.findMaxChapterNumber(seriesId)).thenReturn(1.0);
        when(chapterRepository.existsBySeriesIdAndSlug(seriesId, "chapter-2")).thenReturn(false);
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

        Chapter chapter = chapterService.createDraft(seriesId, creatorId, null);

        assertEquals(2.0, chapter.getChapterNumber());
        assertEquals("chapter-2", chapter.getSlug());
        assertEquals(ChapterState.DRAFT, chapter.getState());
    }

    @Test
    void publishRequiresCopyrightAndIntactMedia() {
        Chapter chapter = Chapter.createDraft(seriesId, 1, "chapter-1", "Ch 1");
        when(seriesService.requireOwned(seriesId, creatorId))
                .thenReturn(Series.create(creatorId, "slug", "Title"));
        when(chapterRepository.findBySeriesIdAndId(seriesId, chapter.getId()))
                .thenReturn(Optional.of(chapter));

        assertThrows(
                ChapterException.class,
                () -> chapterService.publishNow(seriesId, chapter.getId(), creatorId, false)
        );

        doThrow(new ChapterException("Add at least one page before publishing."))
                .when(mediaIntegrityService).requireIntactForPublish(chapter.getId());
        assertThrows(
                ChapterException.class,
                () -> chapterService.publishNow(seriesId, chapter.getId(), creatorId, true)
        );
    }

    @Test
    void publishNowSucceedsWithPagesAndAck() {
        Chapter chapter = Chapter.createDraft(seriesId, 1, "chapter-1", "Ch 1");
        when(seriesService.requireOwned(seriesId, creatorId))
                .thenReturn(Series.create(creatorId, "slug", "Title"));
        when(chapterRepository.findBySeriesIdAndId(seriesId, chapter.getId()))
                .thenReturn(Optional.of(chapter));
        doNothing().when(mediaIntegrityService).requireIntactForPublish(chapter.getId());
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

        Chapter published = chapterService.publishNow(seriesId, chapter.getId(), creatorId, true);

        assertEquals(ChapterState.PUBLISHED, published.getState());
        verify(mediaIntegrityService).requireIntactForPublish(chapter.getId());
        verify(seriesScheduleService).onChapterPublished(eq(seriesId), eq(creatorId), any());
    }

    @Test
    void addPagesRejectsOverCap() {
        Chapter chapter = Chapter.createDraft(seriesId, 1, "chapter-1", "Ch 1");
        when(seriesService.requireOwned(seriesId, creatorId))
                .thenReturn(Series.create(creatorId, "slug", "Title"));
        when(chapterRepository.findBySeriesIdAndId(seriesId, chapter.getId()))
                .thenReturn(Optional.of(chapter));
        when(chapterPageRepository.countByChapterId(chapter.getId())).thenReturn(40L);

        MockMultipartFile file = new MockMultipartFile(
                "files", "a.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        assertThrows(
                ChapterException.class,
                () -> chapterService.addPages(seriesId, chapter.getId(), creatorId, List.of(file))
        );
    }

    @Test
    void slugFromNumberFormatsFloats() {
        assertEquals("chapter-1", ChapterService.slugFromNumber(1));
        assertEquals("chapter-1-5", ChapterService.slugFromNumber(1.5));
    }
}
