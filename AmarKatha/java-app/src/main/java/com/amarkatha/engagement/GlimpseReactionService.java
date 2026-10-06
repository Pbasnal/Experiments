package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.GlimpseReaction;
import com.amarkatha.publishing.GlimpseImageRepository;
import com.amarkatha.publishing.domain.GlimpseImage;
import com.amarkatha.shared.glimpse.GlimpseReactionLookup;
import com.amarkatha.shared.glimpse.ImageReactionState;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GlimpseReactionService implements GlimpseReactionLookup {

    private final GlimpseReactionRepository reactionRepository;
    private final GlimpseImageRepository imageRepository;

    public GlimpseReactionService(
            GlimpseReactionRepository reactionRepository,
            GlimpseImageRepository imageRepository
    ) {
        this.reactionRepository = reactionRepository;
        this.imageRepository = imageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ImageReactionState> forImages(UUID viewerId, Collection<UUID> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = imageIds.stream().distinct().toList();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : reactionRepository.countByImageIdIn(ids)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        Set<UUID> reacted = viewerId == null
                ? Set.of()
                : Set.copyOf(reactionRepository.findImageIdsByUserIdAndImageIdIn(viewerId, ids));
        Map<UUID, ImageReactionState> states = new HashMap<>();
        for (UUID id : ids) {
            states.put(id, new ImageReactionState(counts.getOrDefault(id, 0L), reacted.contains(id)));
        }
        return states;
    }

    @Transactional
    public ImageReactionState react(UUID userId, UUID glimpseId, UUID imageId) {
        requireImage(glimpseId, imageId);
        if (!reactionRepository.existsByUserIdAndImageId(userId, imageId)) {
            reactionRepository.save(GlimpseReaction.create(userId, imageId));
        }
        reactionRepository.flush();
        return stateFor(userId, imageId);
    }

    @Transactional
    public ImageReactionState clear(UUID userId, UUID glimpseId, UUID imageId) {
        requireImage(glimpseId, imageId);
        reactionRepository.deleteByUserIdAndImageId(userId, imageId);
        reactionRepository.flush();
        return stateFor(userId, imageId);
    }

    private GlimpseImage requireImage(UUID glimpseId, UUID imageId) {
        return imageRepository.findByIdAndGlimpseId(imageId, glimpseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
    }

    private ImageReactionState stateFor(UUID userId, UUID imageId) {
        return forImages(userId, List.of(imageId)).get(imageId);
    }
}
