package com.amarkatha.publishing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "chapter_page")
public class ChapterPage {

    @Id
    private UUID id;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "original_storage_key", nullable = false, length = 500)
    private String originalStorageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "webp_storage_key", length = 500)
    private String webpStorageKey;

    private Integer width;

    private Integer height;

    @Column(name = "bytes_original")
    private Long bytesOriginal;

    @Column(name = "bytes_webp")
    private Long bytesWebp;

    protected ChapterPage() {
    }

    public static ChapterPage create(
            UUID chapterId,
            int sortOrder,
            String originalStorageKey,
            String originalFilename,
            Long bytesOriginal,
            Integer width,
            Integer height
    ) {
        ChapterPage page = new ChapterPage();
        page.id = UUID.randomUUID();
        page.chapterId = chapterId;
        page.sortOrder = sortOrder;
        page.originalStorageKey = originalStorageKey;
        page.originalFilename = originalFilename;
        page.bytesOriginal = bytesOriginal;
        page.width = width;
        page.height = height;
        return page;
    }

    public UUID getId() {
        return id;
    }

    public UUID getChapterId() {
        return chapterId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getOriginalStorageKey() {
        return originalStorageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getWebpStorageKey() {
        return webpStorageKey;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Long getBytesOriginal() {
        return bytesOriginal;
    }

    public Long getBytesWebp() {
        return bytesWebp;
    }

    public void applyWebp(String webpStorageKey, Long bytesWebp) {
        this.webpStorageKey = webpStorageKey;
        this.bytesWebp = bytesWebp;
    }
}
