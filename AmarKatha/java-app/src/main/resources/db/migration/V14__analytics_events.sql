-- V14: append-only reader analytics (no PII beyond anonymous reader_id cookie)

CREATE TABLE analytics_event (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(32)  NOT NULL,
    series_id       UUID         NOT NULL,
    chapter_id      UUID,
    reader_id       VARCHAR(36)  NOT NULL,
    referrer        VARCHAR(32)  NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_series_reader_time
    ON analytics_event (series_id, reader_id, occurred_at);

CREATE INDEX idx_analytics_type_occurred
    ON analytics_event (type, occurred_at);
