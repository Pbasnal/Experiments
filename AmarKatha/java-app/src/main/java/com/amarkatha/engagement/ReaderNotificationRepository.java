package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.ReaderNotification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderNotificationRepository extends JpaRepository<ReaderNotification, UUID> {

    List<ReaderNotification> findByUserIdAndInAppVisibleTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndInAppVisibleTrueAndReadAtIsNull(UUID userId);

    Optional<ReaderNotification> findByIdAndUserIdAndInAppVisibleTrue(UUID id, UUID userId);

    Optional<ReaderNotification> findByUserIdAndTypeAndChapterId(UUID userId, String type, UUID chapterId);

    Optional<ReaderNotification> findByUserIdAndTypeAndGlimpseId(UUID userId, String type, UUID glimpseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ReaderNotification n
               set n.readAt = :now
             where n.userId = :userId
               and n.inAppVisible = true
               and n.readAt is null
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("now") java.time.Instant now);
}
