-- Account-owned series follows and reading progress (not anonymous analytics_event)

CREATE TABLE series_follow (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    series_id   UUID         NOT NULL REFERENCES series (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_series_follow_user_series UNIQUE (user_id, series_id)
);

CREATE INDEX idx_series_follow_user_created ON series_follow (user_id, created_at DESC);
CREATE INDEX idx_series_follow_series ON series_follow (series_id);

CREATE TABLE reader_progress (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    series_id        UUID         NOT NULL REFERENCES series (id) ON DELETE CASCADE,
    last_chapter_id  UUID         NOT NULL REFERENCES chapter (id) ON DELETE CASCADE,
    last_read_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_reader_progress_user_series UNIQUE (user_id, series_id)
);

CREATE INDEX idx_reader_progress_user_read ON reader_progress (user_id, last_read_at DESC);
