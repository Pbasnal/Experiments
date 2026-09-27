-- V20: allow product / RUM events without series or chapter context.
-- Forward-only: DROP NOT NULL on series_id; add optional allowlisted meta payload.
-- Existing SERIES_VIEW / CHAPTER_VIEW rows keep series_id populated; indexes unchanged.

ALTER TABLE analytics_event
    ALTER COLUMN series_id DROP NOT NULL;

ALTER TABLE analytics_event
    ADD COLUMN meta_json VARCHAR(512);
