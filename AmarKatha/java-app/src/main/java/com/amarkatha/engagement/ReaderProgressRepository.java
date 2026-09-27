package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.ReaderProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReaderProgressRepository extends JpaRepository<ReaderProgress, UUID> {

    Optional<ReaderProgress> findByUserIdAndSeriesId(UUID userId, UUID seriesId);

    List<ReaderProgress> findByUserIdOrderByLastReadAtDesc(UUID userId);
}
