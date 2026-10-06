package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.Glimpse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlimpseRepository extends JpaRepository<Glimpse, UUID> {

    List<Glimpse> findBySeriesIdOrderByPostedAtAsc(UUID seriesId);

    Optional<Glimpse> findByIdAndSeriesId(UUID id, UUID seriesId);
}
