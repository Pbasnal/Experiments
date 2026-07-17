-- Schedule audit log + creator-set period (days) on series.
-- next_expected_at remains start-of-day Asia/Kolkata stored as TIMESTAMPTZ (UTC).

ALTER TABLE series
    ADD COLUMN IF NOT EXISTS period_days SMALLINT;

ALTER TABLE series
    DROP CONSTRAINT IF EXISTS chk_series_cadence;

ALTER TABLE series
    ADD CONSTRAINT chk_series_cadence
        CHECK (cadence IN ('OFF', 'WEEKLY', 'BIWEEKLY', 'CUSTOM'));

ALTER TABLE series
    DROP CONSTRAINT IF EXISTS chk_series_period_days;

ALTER TABLE series
    ADD CONSTRAINT chk_series_period_days
        CHECK (period_days IS NULL OR (period_days >= 1 AND period_days <= 90));

ALTER TABLE series
    DROP CONSTRAINT IF EXISTS chk_series_dow;

ALTER TABLE series
    ADD CONSTRAINT chk_series_dow
        CHECK (day_of_week IS NULL OR (day_of_week >= 1 AND day_of_week <= 7));

-- Backfill period_days from legacy cadence labels
UPDATE series SET period_days = 7 WHERE cadence = 'WEEKLY' AND period_days IS NULL;
UPDATE series SET period_days = 14 WHERE cadence = 'BIWEEKLY' AND period_days IS NULL;

CREATE TABLE schedule_event (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id                   UUID         NOT NULL REFERENCES series (id) ON DELETE CASCADE,
    event_type                  VARCHAR(32)  NOT NULL,
    message                     VARCHAR(280),
    previous_next_expected_at   TIMESTAMPTZ,
    new_next_expected_at        TIMESTAMPTZ,
    created_by                  UUID REFERENCES app_user (id),
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_schedule_event_type CHECK (event_type IN (
        'ACTIVATE_CADENCE',
        'CLEAR_CADENCE',
        'SKIP',
        'HIATUS',
        'RESUME',
        'PUBLISH_ADVANCE',
        'PUBLISH_EARLY'
    ))
);

CREATE INDEX idx_schedule_event_series_created
    ON schedule_event (series_id, created_at DESC);
