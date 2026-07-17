package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.shared.domain.ChapterState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findBySeriesIdOrderByChapterNumberAsc(UUID seriesId);

    boolean existsBySeriesIdAndSlug(UUID seriesId, String slug);

    @Query("select coalesce(max(c.chapterNumber), 0) from Chapter c where c.seriesId = :seriesId")
    Double findMaxChapterNumber(@Param("seriesId") UUID seriesId);

    long countBySeriesIdAndState(UUID seriesId, ChapterState state);

    Optional<Chapter> findBySeriesIdAndId(UUID seriesId, UUID id);

    Optional<Chapter> findBySeriesIdAndSlugAndState(
            UUID seriesId,
            String slug,
            ChapterState state
    );

    List<Chapter> findBySeriesIdAndStateAndListedAtIsNotNullOrderByChapterNumberAsc(
            UUID seriesId,
            ChapterState state
    );

    long countBySeriesIdAndStateAndListedAtIsNotNull(UUID seriesId, ChapterState state);

    Optional<Chapter> findBySeriesIdAndSlugAndStateAndListedAtIsNotNull(
            UUID seriesId,
            String slug,
            ChapterState state
    );

    List<Chapter> findByState(ChapterState state);
}
