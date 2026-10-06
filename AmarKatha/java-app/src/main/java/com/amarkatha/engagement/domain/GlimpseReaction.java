package com.amarkatha.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "glimpse_reaction")
@IdClass(GlimpseReaction.GlimpseReactionId.class)
public class GlimpseReaction {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "image_id", nullable = false)
    private UUID imageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected GlimpseReaction() {
    }

    public static GlimpseReaction create(UUID userId, UUID imageId) {
        GlimpseReaction reaction = new GlimpseReaction();
        reaction.userId = userId;
        reaction.imageId = imageId;
        reaction.createdAt = Instant.now();
        return reaction;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getImageId() {
        return imageId;
    }

    public static final class GlimpseReactionId implements Serializable {

        private UUID userId;
        private UUID imageId;

        public GlimpseReactionId() {
        }

        public GlimpseReactionId(UUID userId, UUID imageId) {
            this.userId = userId;
            this.imageId = imageId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof GlimpseReactionId that)) {
                return false;
            }
            return Objects.equals(userId, that.userId) && Objects.equals(imageId, that.imageId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, imageId);
        }
    }
}
