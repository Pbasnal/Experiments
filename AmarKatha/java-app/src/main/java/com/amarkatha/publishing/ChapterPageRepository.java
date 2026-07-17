package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.ChapterPage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterPageRepository extends JpaRepository<ChapterPage, UUID> {

    List<ChapterPage> findByChapterIdOrderBySortOrderAsc(UUID chapterId);

    long countByChapterId(UUID chapterId);

    @Query("select coalesce(max(p.sortOrder), 0) from ChapterPage p where p.chapterId = :chapterId")
    Integer findMaxSortOrder(@Param("chapterId") UUID chapterId);
}
