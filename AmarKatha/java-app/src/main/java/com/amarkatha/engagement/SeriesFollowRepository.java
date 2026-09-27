package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.SeriesFollow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeriesFollowRepository extends JpaRepository<SeriesFollow, UUID> {

    boolean existsByUserIdAndSeriesId(UUID userId, UUID seriesId);

    Optional<SeriesFollow> findByUserIdAndSeriesId(UUID userId, UUID seriesId);

    List<SeriesFollow> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<SeriesFollow> findBySeriesId(UUID seriesId);

    long countByUserId(UUID userId);

    void deleteByUserIdAndSeriesId(UUID userId, UUID seriesId);
}
