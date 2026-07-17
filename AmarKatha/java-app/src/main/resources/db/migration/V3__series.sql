CREATE TABLE series (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id          UUID         NOT NULL REFERENCES app_user (id),
    slug                VARCHAR(255) NOT NULL UNIQUE,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    cover_storage_key   VARCHAR(500),
    content_language    VARCHAR(16)  NOT NULL DEFAULT 'en',
    genres              JSONB        NOT NULL DEFAULT '[]'::jsonb,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ONGOING',
    cadence             VARCHAR(32)  NOT NULL DEFAULT 'OFF',
    day_of_week         SMALLINT,
    next_expected_at    TIMESTAMPTZ,
    last_published_at   TIMESTAMPTZ,
    skip_message        VARCHAR(280),
    version             INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_series_status CHECK (status IN ('ONGOING', 'COMPLETED', 'HIATUS')),
    CONSTRAINT chk_series_cadence CHECK (cadence IN ('OFF', 'WEEKLY', 'BIWEEKLY'))
);

CREATE INDEX idx_series_creator_status ON series (creator_id, status);
