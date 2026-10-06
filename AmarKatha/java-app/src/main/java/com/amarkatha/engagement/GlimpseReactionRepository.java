package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.GlimpseReaction;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GlimpseReactionRepository extends JpaRepository<GlimpseReaction, GlimpseReaction.GlimpseReactionId> {

    boolean existsByUserIdAndImageId(UUID userId, UUID imageId);

    void deleteByUserIdAndImageId(UUID userId, UUID imageId);

    @Query("""
            select r.imageId, count(r)
              from GlimpseReaction r
             where r.imageId in :ids
             group by r.imageId
            """)
    List<Object[]> countByImageIdIn(@Param("ids") Collection<UUID> ids);

    @Query("""
            select r.imageId
              from GlimpseReaction r
             where r.userId = :userId
               and r.imageId in :ids
            """)
    List<UUID> findImageIdsByUserIdAndImageIdIn(
            @Param("userId") UUID userId,
            @Param("ids") Collection<UUID> ids
    );
}
