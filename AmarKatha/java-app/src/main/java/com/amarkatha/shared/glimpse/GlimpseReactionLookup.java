package com.amarkatha.shared.glimpse;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Reaction counts for the public series payload. Implemented in engagement so the
 * reader module does not depend on it.
 */
public interface GlimpseReactionLookup {

    Map<UUID, ImageReactionState> forImages(UUID viewerId, Collection<UUID> imageIds);
}
