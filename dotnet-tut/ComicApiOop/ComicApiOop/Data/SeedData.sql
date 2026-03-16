-- Publishers
INSERT INTO Publishers (Name) VALUES 
('Manga Plus'),
('Comic Universe'),
('Digital Comics'),
('Global Comics'),
('Indie Press');

-- Genres
INSERT INTO Genres (Name) VALUES 
('Action'),
('Romance'),
('Fantasy'),
('Sci-Fi'),
('Horror'),
('Comedy'),
('Drama'),
('Mystery'),
('Slice of Life'),
('Adventure');

-- Themes
INSERT INTO Themes (Name) VALUES 
('School Life'),
('Supernatural'),
('Military'),
('Sports'),
('Cyberpunk'),
('Medieval'),
('Space Opera'),
('Post-Apocalyptic'),
('Superhero'),
('Mythology');

-- Customer Segments
INSERT INTO CustomerSegments (Name, IsPremium, IsActive) VALUES 
('Free User', 0, 1),
('Basic Subscriber', 0, 1),
('Premium Member', 1, 1),
('Student', 0, 1),
('VIP', 1, 1);

-- Comics (100 entries)
INSERT INTO Comics (Title, PublisherId, GenreId, ThemeId, TotalChapters, LastUpdateTime, AverageRating)
SELECT 
    CASE 
        WHEN n <= 20 THEN CONCAT('Manga Series #', n)
        WHEN n <= 40 THEN CONCAT('Superhero Comic #', n-20)
        WHEN n <= 60 THEN CONCAT('Fantasy Tale #', n-40)
        WHEN n <= 80 THEN CONCAT('Sci-Fi Adventure #', n-60)
        ELSE CONCAT('Horror Story #', n-80)
    END as Title,
    (n % 5) + 1 as PublisherId,
    (n % 10) + 1 as GenreId,
    (n % 10) + 1 as ThemeId,
    FLOOR(10 + RAND() * 90) as TotalChapters,
    DATE_ADD('2024-01-01', INTERVAL n DAY) as LastUpdateTime,
    4 + RAND() as AverageRating
FROM (
    SELECT 1 + units.i + tens.i * 10 as n
    FROM (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) units,
         (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) tens
    WHERE 1 + units.i + tens.i * 10 <= 100
) numbers;

-- Tags
INSERT INTO Tags (Name) VALUES 
('Popular'),
('New Release'),
('Trending'),
('Award Winner'),
('Editor Choice'),
('Fan Favorite'),
('Classic'),
('Hidden Gem'),
('Best Seller'),
('Rising Star');

-- Link Comics with Tags (each comic gets 2-4 random tags)
INSERT INTO ComicTags (ComicsId, TagsId)
SELECT DISTINCT c.Id, t.tag_id
FROM Comics c
CROSS JOIN (
    SELECT 1 as tag_id, 1 as priority UNION ALL
    SELECT 2, 2 UNION ALL
    SELECT 3, 3 UNION ALL
    SELECT 4, 4 UNION ALL
    SELECT 5, 5 UNION ALL
    SELECT 6, 6 UNION ALL
    SELECT 7, 7 UNION ALL
    SELECT 8, 8 UNION ALL
    SELECT 9, 9 UNION ALL
    SELECT 10, 10
) t
WHERE RAND() < 0.4  -- 40% chance of each tag being assigned
GROUP BY c.Id, t.tag_id;

-- Chapters (create 5-15 chapters for each comic)
INSERT INTO Chapters (ComicId, ChapterNumber, ReleaseTime, IsFree)
SELECT 
    c.Id as ComicId,
    n.num as ChapterNumber,
    DATE_SUB(c.LastUpdateTime, INTERVAL (15 - n.num) DAY) as ReleaseTime,
    CASE 
        WHEN n.num <= 3 THEN 1  -- First 3 chapters free
        ELSE 0
    END as IsFree
FROM Comics c
CROSS JOIN (
    SELECT 1 as num UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
    SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL
    SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
) n;

-- Content Ratings
INSERT INTO ContentRatings (ComicId, AgeRating, ContentFlags, ContentWarning, RequiresParentalGuidance)
SELECT 
    Id as ComicId,
    CASE 
        WHEN Id % 5 = 0 THEN 4  -- Mature
        WHEN Id % 4 = 0 THEN 3  -- Teen15Plus
        WHEN Id % 3 = 0 THEN 2  -- Teen
        ELSE 1                   -- AllAges
    END as AgeRating,
    CASE 
        WHEN Id % 5 = 0 THEN 7   -- Violence + Gore
        WHEN Id % 4 = 0 THEN 1   -- Violence
        WHEN Id % 3 = 0 THEN 32  -- ChildrenFriendly
        ELSE 0                    -- None
    END as ContentFlags,
    CASE 
        WHEN Id % 5 = 0 THEN 'Contains graphic violence and gore'
        WHEN Id % 4 = 0 THEN 'Contains mild violence'
        WHEN Id % 3 = 0 THEN 'Suitable for all ages'
        ELSE ''
    END as ContentWarning,
    CASE 
        WHEN Id % 5 = 0 THEN 1
        ELSE 0
    END as RequiresParentalGuidance
FROM Comics;

-- Geographic Rules (multiple countries per comic)
INSERT INTO GeographicRules (ComicId, IsVisible, LastUpdated, CountryCodes, LicenseStartDate, LicenseEndDate, LicenseType)
SELECT 
    c.Id as ComicId,
    1 as IsVisible,
    NOW() as LastUpdated,
    CASE 
        WHEN c.Id % 4 = 0 THEN 'US,GB,CA,AU'  -- English-speaking countries
        WHEN c.Id % 4 = 1 THEN 'JP,KR,CN'     -- Asian countries
        WHEN c.Id % 4 = 2 THEN 'DE,FR,IT,ES'  -- European countries
        ELSE 'US,GB,JP,KR,CN,DE,FR'          -- Global release
    END as CountryCodes,
    '2024-01-01' as LicenseStartDate,
    '2027-12-31' as LicenseEndDate,
    CASE 
        WHEN c.Id % 3 = 0 THEN 0  -- Full
        ELSE 1                     -- PreviewOnly
    END as LicenseType
FROM Comics c;

-- Customer Segment Rules
INSERT INTO CustomerSegmentRules (ComicId, SegmentId, IsVisible, LastUpdated)
SELECT 
    c.Id as ComicId,
    s.Id as SegmentId,
    CASE 
        WHEN s.IsPremium = 1 THEN 1  -- Always visible to premium
        WHEN c.Id % 3 = 0 THEN 1     -- Some comics visible to all
        ELSE 0                        -- Others restricted
    END as IsVisible,
    NOW() as LastUpdated
FROM Comics c
CROSS JOIN CustomerSegments s;

-- Comic Pricing
INSERT INTO ComicPricings (ComicId, RegionCode, BasePrice, IsFreeContent, IsPremiumContent, DiscountStartDate, DiscountEndDate, DiscountPercentage)
SELECT 
    c.Id as ComicId,
    region.code as RegionCode,
    CASE 
        WHEN region.code IN ('US', 'GB', 'AU') THEN 4.99
        WHEN region.code IN ('JP', 'KR') THEN 500
        ELSE 3.99
    END * (1 + RAND() * 0.2) as BasePrice,
    CASE 
        WHEN c.Id % 10 = 0 THEN 1
        ELSE 0
    END as IsFreeContent,
    CASE 
        WHEN c.Id % 5 = 0 THEN 1
        ELSE 0
    END as IsPremiumContent,
    CASE 
        WHEN c.Id % 4 = 0 THEN '2024-02-01'
        ELSE NULL
    END as DiscountStartDate,
    CASE 
        WHEN c.Id % 4 = 0 THEN '2024-02-28'
        ELSE NULL
    END as DiscountEndDate,
    CASE 
        WHEN c.Id % 4 = 0 THEN 20
        ELSE NULL
    END as DiscountPercentage
FROM Comics c
CROSS JOIN (
    SELECT 'US' as code UNION SELECT 'GB' UNION SELECT 'JP' UNION 
    SELECT 'KR' UNION SELECT 'DE' UNION SELECT 'FR' UNION SELECT 'AU'
) region
WHERE FIND_IN_SET(region.code, (
    SELECT CountryCodes 
    FROM GeographicRules 
    WHERE ComicId = c.Id
)) > 0;
