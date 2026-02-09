-- Database Cleanup Script
-- Deletes all data from all tables in the correct order (respecting foreign key constraints)

-- Disable foreign key checks temporarily for easier cleanup
SET FOREIGN_KEY_CHECKS = 0;

-- Delete data from dependent tables first
DELETE FROM ComputedVisibilities;
DELETE FROM ComicPricing;
DELETE FROM CustomerSegmentRules;
DELETE FROM GeographicRules;
DELETE FROM ContentRatings;
DELETE FROM Chapters;
DELETE FROM ComicTags;

-- Delete main entity data
DELETE FROM Comics;
DELETE FROM Tags;
DELETE FROM CustomerSegments;

-- Delete reference data
DELETE FROM Publishers;
DELETE FROM Genres;
DELETE FROM Themes;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Reset auto-increment counters (optional, but good practice)
ALTER TABLE Comics AUTO_INCREMENT = 1;
ALTER TABLE Publishers AUTO_INCREMENT = 1;
ALTER TABLE Genres AUTO_INCREMENT = 1;
ALTER TABLE Themes AUTO_INCREMENT = 1;
ALTER TABLE Tags AUTO_INCREMENT = 1;
ALTER TABLE CustomerSegments AUTO_INCREMENT = 1;
ALTER TABLE Chapters AUTO_INCREMENT = 1;
ALTER TABLE ContentRatings AUTO_INCREMENT = 1;
ALTER TABLE GeographicRules AUTO_INCREMENT = 1;
ALTER TABLE CustomerSegmentRules AUTO_INCREMENT = 1;
ALTER TABLE ComicPricing AUTO_INCREMENT = 1;
ALTER TABLE ComputedVisibilities AUTO_INCREMENT = 1;

-- Verify cleanup
SELECT 
    (SELECT COUNT(*) FROM Comics) as Comics,
    (SELECT COUNT(*) FROM GeographicRules) as GeographicRules,
    (SELECT COUNT(*) FROM CustomerSegmentRules) as SegmentRules,
    (SELECT COUNT(*) FROM CustomerSegments) as Segments,
    (SELECT COUNT(*) FROM Chapters) as Chapters,
    (SELECT COUNT(*) FROM Publishers) as Publishers,
    (SELECT COUNT(*) FROM Genres) as Genres,
    (SELECT COUNT(*) FROM Themes) as Themes,
    (SELECT COUNT(*) FROM Tags) as Tags,
    (SELECT COUNT(*) FROM ComputedVisibilities) as ComputedVisibilities;