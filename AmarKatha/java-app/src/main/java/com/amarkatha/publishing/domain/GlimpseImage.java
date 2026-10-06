package com.amarkatha.publishing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "glimpse_image")
public class GlimpseImage {

    @Id
    private UUID id;

    @Column(name = "glimpse_id", nullable = false)
    private UUID glimpseId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    private Integer width;

    private Integer height;

    protected GlimpseImage() {
    }

    public static GlimpseImage create(
            UUID glimpseId,
            int sortOrder,
            String storageKey,
            Integer width,
            Integer height
    ) {
        GlimpseImage image = new GlimpseImage();
        image.id = UUID.randomUUID();
        image.glimpseId = glimpseId;
        image.sortOrder = sortOrder;
        image.storageKey = storageKey;
        image.width = width;
        image.height = height;
        return image;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGlimpseId() {
        return glimpseId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }
}
