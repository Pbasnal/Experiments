package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
