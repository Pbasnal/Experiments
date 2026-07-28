-- Preserve creator-provided filenames for chapter page ordering UX

ALTER TABLE chapter_page
    ADD COLUMN original_filename VARCHAR(255);
