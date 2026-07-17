package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.SeriesStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeriesRepository extends JpaRepository<Series, UUID> {

    Optional<Series> findBySlug(String slug);

    long countByCreatorIdAndStatus(UUID creatorId, SeriesStatus status);

    boolean existsBySlug(String slug);
}
