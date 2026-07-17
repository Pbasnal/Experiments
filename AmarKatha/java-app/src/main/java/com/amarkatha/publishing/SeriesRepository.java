package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.SeriesStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeriesRepository extends JpaRepository<Series, UUID> {

    Optional<Series> findBySlug(String slug);

    List<Series> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);

    long countByCreatorIdAndStatus(UUID creatorId, SeriesStatus status);

    boolean existsBySlug(String slug);

    @Query("""
            select s from Series s
            where exists (
                select 1 from Chapter c
                where c.seriesId = s.id
                  and c.state = com.amarkatha.shared.domain.ChapterState.PUBLISHED
                  and c.listedAt is not null
            )
            order by s.lastPublishedAt desc nulls last, s.createdAt desc
            """)
    List<Series> findDiscoverableOrderByRecent();
}
