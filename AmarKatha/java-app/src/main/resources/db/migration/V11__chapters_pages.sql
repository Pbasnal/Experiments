-- Chapters and pages (V0 publishing hierarchy)

CREATE TABLE chapter (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id           UUID         NOT NULL REFERENCES series (id) ON DELETE CASCADE,
    chapter_number      DOUBLE PRECISION NOT NULL,
    title               VARCHAR(500),
    state               VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    slug                VARCHAR(255) NOT NULL,
    scheduled_at        TIMESTAMPTZ,
    published_at        TIMESTAMPTZ,
    listed_at           TIMESTAMPTZ,
    list_early          BOOLEAN      NOT NULL DEFAULT FALSE,
    copyright_ack_at    TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_chapter_state CHECK (state IN ('DRAFT', 'SCHEDULED', 'PUBLISHED')),
    CONSTRAINT uq_chapter_series_slug UNIQUE (series_id, slug)
);

CREATE INDEX idx_chapter_series_listed ON chapter (series_id, listed_at DESC);

-- Unique chapter numbers among non-draft chapters in a series
CREATE UNIQUE INDEX uq_chapter_series_number_non_draft
    ON chapter (series_id, chapter_number)
    WHERE state <> 'DRAFT';

CREATE TABLE chapter_page (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id            UUID         NOT NULL REFERENCES chapter (id) ON DELETE CASCADE,
    sort_order            INT          NOT NULL,
    original_storage_key  VARCHAR(500) NOT NULL,
    webp_storage_key      VARCHAR(500),
    width                 INT,
    height                INT,
    bytes_original        BIGINT,
    bytes_webp            BIGINT,
    CONSTRAINT uq_chapter_page_order UNIQUE (chapter_id, sort_order)
);

CREATE INDEX idx_chapter_page_chapter ON chapter_page (chapter_id, sort_order);
