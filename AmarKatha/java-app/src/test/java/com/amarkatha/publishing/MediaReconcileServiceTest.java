package com.amarkatha.publishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.shared.domain.ChapterState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaReconcileServiceTest {

    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ChapterMediaIntegrityService integrityService;

    private MediaReconcileService reconcileService;

    @BeforeEach
    void setUp() {
        reconcileService = new MediaReconcileService(chapterRepository, integrityService);
    }

    @Test
    void delistsBrokenListedAndRelistsRepaired() {
        UUID seriesId = UUID.randomUUID();
        Chapter broken = Chapter.createDraft(seriesId, 1, "broken", "Broken");
        broken.publishNow(Instant.now(), Instant.now());
        Chapter repaired = Chapter.createDraft(seriesId, 2, "repaired", "Repaired");
        repaired.publishNow(Instant.now(), Instant.now());
        repaired.delist();
        Chapter ok = Chapter.createDraft(seriesId, 3, "ok", "Ok");
        ok.publishNow(Instant.now(), Instant.now());

        when(chapterRepository.findByState(ChapterState.PUBLISHED))
                .thenReturn(List.of(broken, repaired, ok));
        when(integrityService.isChapterMediaIntact(broken.getId())).thenReturn(false);
        when(integrityService.findMissingKeys(broken.getId())).thenReturn(List.of("missing-key"));
        when(integrityService.isChapterMediaIntact(repaired.getId())).thenReturn(true);
        when(integrityService.isChapterMediaIntact(ok.getId())).thenReturn(true);
        when(chapterRepository.save(broken)).thenReturn(broken);
        when(chapterRepository.save(repaired)).thenReturn(repaired);

        MediaReconcileService.ReconcileReport report = reconcileService.reconcile();

        assertEquals(1, report.delisted().size());
        assertEquals(broken.getId(), report.delisted().get(0).chapterId());
        assertEquals(1, report.relisted().size());
        assertEquals(repaired.getId(), report.relisted().get(0));
        assertEquals(1, report.intactListedCount());
        Assertions.assertNull(broken.getListedAt());
        assertTrue(repaired.getListedAt() != null);
        verify(chapterRepository).save(broken);
        verify(chapterRepository).save(repaired);
    }
}
