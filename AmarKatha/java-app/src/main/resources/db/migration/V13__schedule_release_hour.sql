-- Creator-set release hour (IST), hour granularity (minutes always :00).

ALTER TABLE series
    ADD COLUMN IF NOT EXISTS release_hour_ist SMALLINT;

ALTER TABLE series
    DROP CONSTRAINT IF EXISTS chk_series_release_hour;

ALTER TABLE series
    ADD CONSTRAINT chk_series_release_hour
        CHECK (release_hour_ist IS NULL OR (release_hour_ist >= 0 AND release_hour_ist <= 23));

-- Default evening slot for series that already have an active period
UPDATE series
SET release_hour_ist = 18
WHERE period_days IS NOT NULL
  AND release_hour_ist IS NULL;
