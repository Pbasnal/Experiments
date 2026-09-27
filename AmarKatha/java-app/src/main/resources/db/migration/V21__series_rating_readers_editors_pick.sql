ALTER TABLE series
    ADD COLUMN rating NUMERIC(2, 1) NOT NULL DEFAULT 0,
    ADD COLUMN reader_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN editors_pick BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE series
    ADD CONSTRAINT chk_series_rating CHECK (rating >= 0 AND rating <= 5),
    ADD CONSTRAINT chk_series_reader_count CHECK (reader_count >= 0);
