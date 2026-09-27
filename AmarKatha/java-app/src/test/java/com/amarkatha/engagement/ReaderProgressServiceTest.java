package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.business.ReadingEntryInput;
import com.amarkatha.business.ReadingEntryInstruction;
import com.amarkatha.business.ReadingEntryPolicy;
import com.amarkatha.engagement.domain.ReaderProgress;
import com.amarkatha.engagement.dto.ProgressUpdateRequest;
import com.amarkatha.engagement.dto.ReaderProgressDto;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.ChapterState;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReaderProgressServiceTest {

    @Mock
    private ReaderProgressRepository readerProgressRepository;
    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ReadingEntryPolicy readingEntryPolicy;

    @InjectMocks
    private ReaderProgressService readerProgressService;

    private UUID userId;
    private Series series;
    private Chapter chapter;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        series = Series.create(UUID.randomUUID(), "demo-series", "Demo");
        chapter = Chapter.createDraft(series.getId(), 1.0, "ch-1", "Chapter 1");
        chapter.publishNow(Instant.now(), Instant.now());
    }

    @Test
    void recordReadCreatesProgress() {
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));
        when(chapterRepository.findBySeriesIdAndSlugAndStateAndListedAtIsNotNull(
                series.getId(), "ch-1", ChapterState.PUBLISHED
        )).thenReturn(Optional.of(chapter));
        when(readerProgressRepository.findByUserIdAndSeriesId(userId, series.getId()))
                .thenReturn(Optional.empty());

        ReaderProgressDto dto = readerProgressService.recordRead(
                userId,
                new ProgressUpdateRequest("demo-series", "ch-1")
        );

        assertEquals("demo-series", dto.seriesSlug());
        assertEquals("ch-1", dto.chapterSlug());
        ArgumentCaptor<ReaderProgress> captor = ArgumentCaptor.forClass(ReaderProgress.class);
        verify(readerProgressRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals(chapter.getId(), captor.getValue().getLastChapterId());
    }

    @Test
    void recordReadUpdatesExistingProgress() {
        ReaderProgress existing = ReaderProgress.create(userId, series.getId(), UUID.randomUUID());
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));
        when(chapterRepository.findBySeriesIdAndSlugAndStateAndListedAtIsNotNull(
                series.getId(), "ch-1", ChapterState.PUBLISHED
        )).thenReturn(Optional.of(chapter));
        when(readerProgressRepository.findByUserIdAndSeriesId(userId, series.getId()))
                .thenReturn(Optional.of(existing));

        readerProgressService.recordRead(userId, "demo-series", "ch-1");

        assertEquals(chapter.getId(), existing.getLastChapterId());
        verify(readerProgressRepository).save(existing);
    }

    @Test
    void recordReadRejectsBlankBody() {
        assertThrows(
                ResponseStatusException.class,
                () -> readerProgressService.recordRead(userId, new ProgressUpdateRequest(" ", "ch-1"))
        );
        verify(seriesRepository, never()).findBySlug(any());
    }

    @Test
    void readTargetResumesLastPublishedChapter() {
        ReaderProgress progress = ReaderProgress.create(userId, series.getId(), chapter.getId());
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));
        when(readerProgressRepository.findByUserIdAndSeriesId(userId, series.getId()))
                .thenReturn(Optional.of(progress));
        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
        when(readingEntryPolicy.decide(new ReadingEntryInput(true)))
                .thenReturn(ReadingEntryInstruction.RESUME_LAST_CHAPTER);

        var target = readerProgressService.readTarget(userId, "demo-series");

        assertEquals("/read/s/demo-series/c/ch-1", target.href());
        assertEquals(true, target.resumed());
    }

    @Test
    void readTargetOpensSeriesForFirstVisit() {
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));
        when(readerProgressRepository.findByUserIdAndSeriesId(userId, series.getId()))
                .thenReturn(Optional.empty());
        when(readingEntryPolicy.decide(new ReadingEntryInput(false)))
                .thenReturn(ReadingEntryInstruction.OPEN_SERIES);

        var target = readerProgressService.readTarget(userId, "demo-series");

        assertEquals("/read/s/demo-series", target.href());
        assertEquals(false, target.resumed());
    }
}
