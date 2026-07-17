package com.amarkatha.publishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.media.MediaStore;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.SeriesStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeriesServiceTest {

    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private MediaStore mediaStore;
    @Mock
    private WebpConversionService webpConversionService;

    private SeriesService seriesService;

    @BeforeEach
    void setUp() {
        seriesService = new SeriesService(
                seriesRepository,
                mediaStore,
                webpConversionService,
                16 * 1024 * 1024
        );
    }

    @Test
    void createSeriesEnforcesOngoingCap() {
        UUID creatorId = UUID.randomUUID();
        when(seriesRepository.countByCreatorIdAndStatus(creatorId, SeriesStatus.ONGOING))
                .thenReturn(5L);

        assertThrows(
                OngoingSeriesCapExceededException.class,
                () -> seriesService.createSeries(creatorId, "Title", null, "en")
        );
    }

    @Test
    void createSeriesSavesWithSlug() {
        UUID creatorId = UUID.randomUUID();
        when(seriesRepository.countByCreatorIdAndStatus(creatorId, SeriesStatus.ONGOING))
                .thenReturn(0L);
        when(seriesRepository.existsBySlug("midnight-metro")).thenReturn(false);
        when(seriesRepository.save(any(Series.class))).thenAnswer(inv -> inv.getArgument(0));

        Series series = seriesService.createSeries(creatorId, "Midnight Metro", "A blurb", "hi");

        assertEquals("Midnight Metro", series.getTitle());
        assertEquals("midnight-metro", series.getSlug());
        assertEquals("hi", series.getContentLanguage());
        assertEquals("A blurb", series.getDescription());
        verify(seriesRepository).save(any(Series.class));
    }

    @Test
    void requireOwnedRejectsOtherCreator() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Series series = Series.create(owner, "slug", "Title");
        when(seriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        assertThrows(SeriesAccessException.class, () -> seriesService.requireOwned(series.getId(), other));
    }

    @Test
    void updateSeriesChangesMetadata() {
        UUID owner = UUID.randomUUID();
        Series series = Series.create(owner, "slug", "Old");
        when(seriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenAnswer(inv -> inv.getArgument(0));

        Series updated = seriesService.updateSeries(series.getId(), owner, "New Title", "Desc", "ta");

        assertEquals("New Title", updated.getTitle());
        assertEquals("Desc", updated.getDescription());
        assertEquals("ta", updated.getContentLanguage());
        ArgumentCaptor<Series> captor = ArgumentCaptor.forClass(Series.class);
        verify(seriesRepository).save(captor.capture());
        assertEquals("New Title", captor.getValue().getTitle());
    }
}
