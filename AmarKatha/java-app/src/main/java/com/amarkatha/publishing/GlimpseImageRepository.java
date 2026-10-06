package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.GlimpseImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlimpseImageRepository extends JpaRepository<GlimpseImage, UUID> {

    List<GlimpseImage> findByGlimpseIdInOrderBySortOrderAsc(Collection<UUID> glimpseIds);

    Optional<GlimpseImage> findByIdAndGlimpseId(UUID id, UUID glimpseId);

    long countByGlimpseId(UUID glimpseId);
}
