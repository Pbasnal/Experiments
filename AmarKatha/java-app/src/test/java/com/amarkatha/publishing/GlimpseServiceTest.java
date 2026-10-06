package com.amarkatha.publishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.business.GlimpsePostPolicy;
import com.amarkatha.media.MediaStore;
import com.amarkatha.outbox.DomainEventPublisher;
import com.amarkatha.outbox.DomainEventTypes;
import com.amarkatha.publishing.domain.GlimpseImage;
import com.amarkatha.publishing.domain.Series;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class GlimpseServiceTest {

    @Mock
    private SeriesService seriesService;
    @Mock
    private GlimpseRepository glimpseRepository;
    @Mock
    private GlimpseImageRepository imageRepository;
    @Mock
    private MediaStore mediaStore;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    private GlimpseService service;

    @BeforeEach
    void setUp() {
        service = new GlimpseService(
                seriesService,
                glimpseRepository,
                imageRepository,
                mediaStore,
                domainEventPublisher,
                new GlimpsePostPolicy(),
                8_000_000
        );
    }

    @Test
    void publishWritesOutboxAndStoresImagesInOrder() {
        UUID creatorId = UUID.randomUUID();
        Series series = Series.create(creatorId, "monsoon-market", "Monsoon Market");
        when(seriesService.requireOwned(series.getId(), creatorId)).thenReturn(series);
        when(mediaStore.putOriginal(any(), any(), any()))
                .thenAnswer(invocation -> new MediaStore.UploadResult(invocation.getArgument(0), 12));
        when(glimpseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.publish(
                series.getId(),
                creatorId,
                "CHARACTER",
                List.of(
                        new MockMultipartFile("images", "front.jpg", "image/jpeg", new byte[] {1, 2, 3}),
                        new MockMultipartFile("images", "side.png", "image/png", new byte[] {4, 5})
                ),
                true
        );

        ArgumentCaptor<GlimpseImage> images = ArgumentCaptor.forClass(GlimpseImage.class);
        verify(imageRepository, times(2)).save(images.capture());
        assertEquals(1, images.getAllValues().get(0).getSortOrder());
        assertEquals(2, images.getAllValues().get(1).getSortOrder());
        assertTrue(images.getAllValues().get(0).getStorageKey().endsWith("/1.jpg"));
        assertTrue(images.getAllValues().get(1).getStorageKey().endsWith("/2.png"));
        assertEquals(
                images.getAllValues().get(0).getGlimpseId(),
                images.getAllValues().get(1).getGlimpseId()
        );

        verify(domainEventPublisher).append(
                eq(DomainEventTypes.GLIMPSE_PUBLISHED),
                eq("glimpse"),
                eq(images.getAllValues().get(0).getGlimpseId()),
                eq(DomainEventTypes.GLIMPSE_PUBLISHED + ":" + images.getAllValues().get(0).getGlimpseId()),
                any()
        );
    }
}
