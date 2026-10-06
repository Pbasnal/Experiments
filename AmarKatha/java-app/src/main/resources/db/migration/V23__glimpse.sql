-- Free multi-image posts on a series, plus reactions and follower notices.

CREATE TABLE glimpse (
    id                UUID PRIMARY KEY,
    series_id         UUID         NOT NULL REFERENCES series (id) ON DELETE CASCADE,
    tag               VARCHAR(32)  NOT NULL,
    posted_at         TIMESTAMPTZ  NOT NULL,
    copyright_ack_at  TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_glimpse_tag CHECK (tag IN ('CHARACTER', 'BACKGROUND', 'LORE', 'ITEMS', 'TEASER'))
);

CREATE INDEX idx_glimpse_series_posted
    ON glimpse (series_id, posted_at);

CREATE TABLE glimpse_image (
    id           UUID PRIMARY KEY,
    glimpse_id   UUID         NOT NULL REFERENCES glimpse (id) ON DELETE CASCADE,
    sort_order   INT          NOT NULL,
    storage_key  VARCHAR(500) NOT NULL,
    width        INT,
    height       INT,
    CONSTRAINT uq_glimpse_image_order UNIQUE (glimpse_id, sort_order)
);

CREATE INDEX idx_glimpse_image_glimpse
    ON glimpse_image (glimpse_id, sort_order);

CREATE TABLE glimpse_reaction (
    user_id     UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    image_id    UUID        NOT NULL REFERENCES glimpse_image (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, image_id)
);

CREATE INDEX idx_glimpse_reaction_image
    ON glimpse_reaction (image_id);

-- A notice is either a chapter or a glimpse. chapter_id stays for existing rows.
ALTER TABLE reader_notification
    ALTER COLUMN chapter_id DROP NOT NULL;

ALTER TABLE reader_notification
    ADD COLUMN glimpse_id UUID NULL REFERENCES glimpse (id) ON DELETE CASCADE;

ALTER TABLE reader_notification
    DROP CONSTRAINT uq_reader_notification_user_type_chapter;

CREATE UNIQUE INDEX uq_reader_notification_user_type_chapter
    ON reader_notification (user_id, type, chapter_id)
    WHERE chapter_id IS NOT NULL;

CREATE UNIQUE INDEX uq_reader_notification_user_type_glimpse
    ON reader_notification (user_id, type, glimpse_id)
    WHERE glimpse_id IS NOT NULL;

ALTER TABLE reader_notification
    ADD CONSTRAINT chk_reader_notification_subject CHECK (
        (chapter_id IS NOT NULL AND glimpse_id IS NULL)
        OR (chapter_id IS NULL AND glimpse_id IS NOT NULL)
    );
