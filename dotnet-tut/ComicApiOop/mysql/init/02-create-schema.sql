-- Create database if not exists
-- Note: Database is already created by MySQL from MYSQL_DATABASE env var
-- This script uses the database from the environment variable
USE ComicBookDb;

-- Create Publishers table
CREATE TABLE IF NOT EXISTS Publishers (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(255) NOT NULL
);

-- Create Genres table
CREATE TABLE IF NOT EXISTS Genres (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(255) NOT NULL
);

-- Create Themes table
CREATE TABLE IF NOT EXISTS Themes (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(255) NOT NULL
);

-- Create Comics table
CREATE TABLE IF NOT EXISTS Comics (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Title VARCHAR(255) NOT NULL,
    PublisherId BIGINT NOT NULL,
    GenreId BIGINT NOT NULL,
    ThemeId BIGINT NOT NULL,
    TotalChapters INT NOT NULL,
    LastUpdateTime DATETIME(6) NOT NULL,
    AverageRating DOUBLE NOT NULL,
    FOREIGN KEY (PublisherId) REFERENCES Publishers(Id),
    FOREIGN KEY (GenreId) REFERENCES Genres(Id),
    FOREIGN KEY (ThemeId) REFERENCES Themes(Id)
);

-- Create Chapters table
CREATE TABLE IF NOT EXISTS Chapters (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ComicId BIGINT NOT NULL,
    ChapterNumber INT NOT NULL,
    ReleaseTime DATETIME(6) NOT NULL,
    IsFree TINYINT(1) NOT NULL,
    FOREIGN KEY (ComicId) REFERENCES Comics(Id)
);

-- Create Tags table
CREATE TABLE IF NOT EXISTS Tags (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(50) NOT NULL
);

-- Create ComicTag join table
CREATE TABLE IF NOT EXISTS ComicTag (
    ComicsId BIGINT NOT NULL,
    TagsId BIGINT NOT NULL,
    PRIMARY KEY (ComicsId, TagsId),
    CONSTRAINT FK_ComicTag_Comics FOREIGN KEY (ComicsId) REFERENCES Comics(Id) ON DELETE CASCADE,
    CONSTRAINT FK_ComicTag_Tags FOREIGN KEY (TagsId) REFERENCES Tags(Id) ON DELETE CASCADE
);

-- Create CustomerSegments table
CREATE TABLE IF NOT EXISTS CustomerSegments (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(255) NOT NULL,
    IsPremium TINYINT(1) NOT NULL,
    IsActive TINYINT(1) NOT NULL
);

-- Create GeographicRules table
CREATE TABLE IF NOT EXISTS GeographicRules (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ComicId BIGINT NOT NULL,
    IsVisible TINYINT(1) NOT NULL,
    LastUpdated DATETIME(6) NOT NULL,
    CountryCodes VARCHAR(1000) NOT NULL,
    LicenseStartDate DATETIME(6) NOT NULL,
    LicenseEndDate DATETIME(6) NOT NULL,
    LicenseType INT NOT NULL,
    FOREIGN KEY (ComicId) REFERENCES Comics(Id)
);

-- Create CustomerSegmentRules table
CREATE TABLE IF NOT EXISTS CustomerSegmentRules (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ComicId BIGINT NOT NULL,
    SegmentId BIGINT NOT NULL,
    IsVisible TINYINT(1) NOT NULL,
    LastUpdated DATETIME(6) NOT NULL,
    FOREIGN KEY (ComicId) REFERENCES Comics(Id),
    FOREIGN KEY (SegmentId) REFERENCES CustomerSegments(Id)
);

-- Create ContentRatings table
CREATE TABLE IF NOT EXISTS ContentRatings (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ComicId BIGINT NOT NULL,
    AgeRating INT NOT NULL,
    ContentFlags INT NOT NULL,
    ContentWarning VARCHAR(500) NOT NULL,
    RequiresParentalGuidance TINYINT(1) NOT NULL,
    FOREIGN KEY (ComicId) REFERENCES Comics(Id)
);

-- Create ComicPricing table
CREATE TABLE IF NOT EXISTS ComicPricing (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ComicId BIGINT NOT NULL,
    RegionCode VARCHAR(2) NOT NULL,
    BasePrice DECIMAL(10,2) NOT NULL,
    IsFreeContent TINYINT(1) NOT NULL,
    IsPremiumContent TINYINT(1) NOT NULL,
    DiscountStartDate DATETIME(6),
    DiscountEndDate DATETIME(6),
    DiscountPercentage DECIMAL(5,2),
    FOREIGN KEY (ComicId) REFERENCES Comics(Id)
);

-- Create ComputedVisibilities table
CREATE TABLE IF NOT EXISTS ComputedVisibilities (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ComicId BIGINT NOT NULL,
    CountryCode VARCHAR(2) NOT NULL,
    CustomerSegmentId BIGINT NOT NULL,
    FreeChaptersCount INT NOT NULL,
    LastChapterReleaseTime DATETIME(6) NOT NULL,
    GenreId BIGINT NOT NULL,
    PublisherId BIGINT NOT NULL,
    AverageRating DOUBLE NOT NULL,
    SearchTags VARCHAR(1000) NOT NULL,
    IsVisible TINYINT(1) NOT NULL,
    ComputedAt DATETIME(6) NOT NULL,
    LicenseType INT NOT NULL,
    CurrentPrice DECIMAL(65,30) NOT NULL,
    IsFreeContent TINYINT(1) NOT NULL,
    IsPremiumContent TINYINT(1) NOT NULL,
    AgeRating INT NOT NULL,
    ContentFlags INT NOT NULL,
    ContentWarning LONGTEXT NOT NULL,
    FOREIGN KEY (ComicId) REFERENCES Comics(Id),
    FOREIGN KEY (CustomerSegmentId) REFERENCES CustomerSegments(Id)
);
