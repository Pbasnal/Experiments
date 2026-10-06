package com.amarkatha.shared.glimpse;

/**
 * Reaction count for one glimpse image, plus whether the current viewer has reacted.
 */
public record ImageReactionState(long count, boolean reacted) {
}
