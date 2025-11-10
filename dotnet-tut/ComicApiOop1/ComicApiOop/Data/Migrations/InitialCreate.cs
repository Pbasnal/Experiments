using Microsoft.EntityFrameworkCore.Metadata;
using Microsoft.EntityFrameworkCore.Migrations;

namespace ComicApiOop.Data.Migrations;

public partial class InitialCreate : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        // Publishers table
        migrationBuilder.CreateTable(
            name: "Publishers",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                Name = table.Column<string>(type: "varchar(255)", maxLength: 255, nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_Publishers", x => x.Id);
            });

        // Genres table
        migrationBuilder.CreateTable(
            name: "Genres",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                Name = table.Column<string>(type: "varchar(255)", maxLength: 255, nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_Genres", x => x.Id);
            });

        // Themes table
        migrationBuilder.CreateTable(
            name: "Themes",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                Name = table.Column<string>(type: "varchar(255)", maxLength: 255, nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_Themes", x => x.Id);
            });

        // Comics table
        migrationBuilder.CreateTable(
            name: "Comics",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                Title = table.Column<string>(type: "varchar(255)", maxLength: 255, nullable: false),
                PublisherId = table.Column<long>(type: "bigint", nullable: false),
                GenreId = table.Column<long>(type: "bigint", nullable: false),
                ThemeId = table.Column<long>(type: "bigint", nullable: false),
                TotalChapters = table.Column<int>(type: "int", nullable: false),
                LastUpdateTime = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                AverageRating = table.Column<double>(type: "double", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_Comics", x => x.Id);
                table.ForeignKey(
                    name: "FK_Comics_Publishers_PublisherId",
                    column: x => x.PublisherId,
                    principalTable: "Publishers",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
                table.ForeignKey(
                    name: "FK_Comics_Genres_GenreId",
                    column: x => x.GenreId,
                    principalTable: "Genres",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
                table.ForeignKey(
                    name: "FK_Comics_Themes_ThemeId",
                    column: x => x.ThemeId,
                    principalTable: "Themes",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        // Chapters table
        migrationBuilder.CreateTable(
            name: "Chapters",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                ComicId = table.Column<long>(type: "bigint", nullable: false),
                ChapterNumber = table.Column<int>(type: "int", nullable: false),
                ReleaseTime = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                IsFree = table.Column<bool>(type: "tinyint(1)", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_Chapters", x => x.Id);
                table.ForeignKey(
                    name: "FK_Chapters_Comics_ComicId",
                    column: x => x.ComicId,
                    principalTable: "Comics",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        // Tags table
        migrationBuilder.CreateTable(
            name: "Tags",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                Name = table.Column<string>(type: "varchar(50)", maxLength: 50, nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_Tags", x => x.Id);
            });

        // Comic-Tag relationship table
        migrationBuilder.CreateTable(
            name: "ComicTag",
            columns: table => new
            {
                ComicsId = table.Column<long>(type: "bigint", nullable: false),
                TagsId = table.Column<long>(type: "bigint", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_ComicTag", x => new { x.ComicsId, x.TagsId });
                table.ForeignKey(
                    name: "FK_ComicTag_Comics_ComicsId",
                    column: x => x.ComicsId,
                    principalTable: "Comics",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
                table.ForeignKey(
                    name: "FK_ComicTag_Tags_TagsId",
                    column: x => x.TagsId,
                    principalTable: "Tags",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        // CustomerSegments table
        migrationBuilder.CreateTable(
            name: "CustomerSegments",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                Name = table.Column<string>(type: "varchar(255)", maxLength: 255, nullable: false),
                IsPremium = table.Column<bool>(type: "tinyint(1)", nullable: false),
                IsActive = table.Column<bool>(type: "tinyint(1)", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_CustomerSegments", x => x.Id);
            });

        // GeographicRules table
        migrationBuilder.CreateTable(
            name: "GeographicRules",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                ComicId = table.Column<long>(type: "bigint", nullable: false),
                IsVisible = table.Column<bool>(type: "tinyint(1)", nullable: false),
                LastUpdated = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                CountryCodes = table.Column<string>(type: "varchar(1000)", maxLength: 1000, nullable: false),
                LicenseStartDate = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                LicenseEndDate = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                LicenseType = table.Column<int>(type: "int", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_GeographicRules", x => x.Id);
                table.ForeignKey(
                    name: "FK_GeographicRules_Comics_ComicId",
                    column: x => x.ComicId,
                    principalTable: "Comics",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        // CustomerSegmentRules table
        migrationBuilder.CreateTable(
            name: "CustomerSegmentRules",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                ComicId = table.Column<long>(type: "bigint", nullable: false),
                SegmentId = table.Column<long>(type: "bigint", nullable: false),
                IsVisible = table.Column<bool>(type: "tinyint(1)", nullable: false),
                LastUpdated = table.Column<DateTime>(type: "datetime(6)", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_CustomerSegmentRules", x => x.Id);
                table.ForeignKey(
                    name: "FK_CustomerSegmentRules_Comics_ComicId",
                    column: x => x.ComicId,
                    principalTable: "Comics",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
                table.ForeignKey(
                    name: "FK_CustomerSegmentRules_CustomerSegments_SegmentId",
                    column: x => x.SegmentId,
                    principalTable: "CustomerSegments",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        // ContentRatings table
        migrationBuilder.CreateTable(
            name: "ContentRatings",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                ComicId = table.Column<long>(type: "bigint", nullable: false),
                AgeRating = table.Column<int>(type: "int", nullable: false),
                ContentFlags = table.Column<int>(type: "int", nullable: false),
                ContentWarning = table.Column<string>(type: "varchar(500)", maxLength: 500, nullable: false),
                RequiresParentalGuidance = table.Column<bool>(type: "tinyint(1)", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_ContentRatings", x => x.Id);
                table.ForeignKey(
                    name: "FK_ContentRatings_Comics_ComicId",
                    column: x => x.ComicId,
                    principalTable: "Comics",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        // ComicPricing table
        migrationBuilder.CreateTable(
            name: "ComicPricing",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                ComicId = table.Column<long>(type: "bigint", nullable: false),
                RegionCode = table.Column<string>(type: "varchar(2)", maxLength: 2, nullable: false),
                BasePrice = table.Column<decimal>(type: "decimal(10,2)", precision: 10, scale: 2, nullable: false),
                IsFreeContent = table.Column<bool>(type: "tinyint(1)", nullable: false),
                IsPremiumContent = table.Column<bool>(type: "tinyint(1)", nullable: false),
                DiscountStartDate = table.Column<DateTime>(type: "datetime(6)", nullable: true),
                DiscountEndDate = table.Column<DateTime>(type: "datetime(6)", nullable: true),
                DiscountPercentage = table.Column<decimal>(type: "decimal(5,2)", precision: 5, scale: 2, nullable: true)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_ComicPricing", x => x.Id);
                table.ForeignKey(
                    name: "FK_ComicPricing_Comics_ComicId",
                    column: x => x.ComicId,
                    principalTable: "Comics",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        // ComputedVisibilities table
        migrationBuilder.CreateTable(
            name: "ComputedVisibilities",
            columns: table => new
            {
                Id = table.Column<long>(type: "bigint", nullable: false)
                    .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                ComicId = table.Column<long>(type: "bigint", nullable: false),
                CountryCode = table.Column<string>(type: "varchar(2)", maxLength: 2, nullable: false),
                CustomerSegmentId = table.Column<long>(type: "bigint", nullable: false),
                FreeChaptersCount = table.Column<int>(type: "int", nullable: false),
                LastChapterReleaseTime = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                GenreId = table.Column<long>(type: "bigint", nullable: false),
                PublisherId = table.Column<long>(type: "bigint", nullable: false),
                AverageRating = table.Column<double>(type: "double", nullable: false),
                SearchTags = table.Column<string>(type: "varchar(1000)", maxLength: 1000, nullable: false),
                IsVisible = table.Column<bool>(type: "tinyint(1)", nullable: false),
                ComputedAt = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                LicenseType = table.Column<int>(type: "int", nullable: false),
                CurrentPrice = table.Column<decimal>(type: "decimal(65,30)", nullable: false),
                IsFreeContent = table.Column<bool>(type: "tinyint(1)", nullable: false),
                IsPremiumContent = table.Column<bool>(type: "tinyint(1)", nullable: false),
                AgeRating = table.Column<int>(type: "int", nullable: false),
                ContentFlags = table.Column<int>(type: "int", nullable: false),
                ContentWarning = table.Column<string>(type: "longtext", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_ComputedVisibilities", x => x.Id);
                table.ForeignKey(
                    name: "FK_ComputedVisibilities_Comics_ComicId",
                    column: x => x.ComicId,
                    principalTable: "Comics",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
                table.ForeignKey(
                    name: "FK_ComputedVisibilities_CustomerSegments_CustomerSegmentId",
                    column: x => x.CustomerSegmentId,
                    principalTable: "CustomerSegments",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        // Create indexes
        migrationBuilder.CreateIndex(
            name: "IX_Chapters_ComicId",
            table: "Chapters",
            column: "ComicId");

        migrationBuilder.CreateIndex(
            name: "IX_ComicPricing_ComicId",
            table: "ComicPricing",
            column: "ComicId");

        migrationBuilder.CreateIndex(
            name: "IX_Comics_GenreId",
            table: "Comics",
            column: "GenreId");

        migrationBuilder.CreateIndex(
            name: "IX_Comics_PublisherId",
            table: "Comics",
            column: "PublisherId");

        migrationBuilder.CreateIndex(
            name: "IX_Comics_ThemeId",
            table: "Comics",
            column: "ThemeId");

        migrationBuilder.CreateIndex(
            name: "IX_ComicTag_TagsId",
            table: "ComicTag",
            column: "TagsId");

        migrationBuilder.CreateIndex(
            name: "IX_ComputedVisibilities_ComicId",
            table: "ComputedVisibilities",
            column: "ComicId");

        migrationBuilder.CreateIndex(
            name: "IX_ComputedVisibilities_CustomerSegmentId",
            table: "ComputedVisibilities",
            column: "CustomerSegmentId");

        migrationBuilder.CreateIndex(
            name: "IX_ContentRatings_ComicId",
            table: "ContentRatings",
            column: "ComicId",
            unique: true);

        migrationBuilder.CreateIndex(
            name: "IX_CustomerSegmentRules_ComicId",
            table: "CustomerSegmentRules",
            column: "ComicId");

        migrationBuilder.CreateIndex(
            name: "IX_CustomerSegmentRules_SegmentId",
            table: "CustomerSegmentRules",
            column: "SegmentId");

        migrationBuilder.CreateIndex(
            name: "IX_GeographicRules_ComicId",
            table: "GeographicRules",
            column: "ComicId");
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropTable(name: "Chapters");
        migrationBuilder.DropTable(name: "ComicPricing");
        migrationBuilder.DropTable(name: "ComicTag");
        migrationBuilder.DropTable(name: "ComputedVisibilities");
        migrationBuilder.DropTable(name: "ContentRatings");
        migrationBuilder.DropTable(name: "CustomerSegmentRules");
        migrationBuilder.DropTable(name: "GeographicRules");
        migrationBuilder.DropTable(name: "Tags");
        migrationBuilder.DropTable(name: "CustomerSegments");
        migrationBuilder.DropTable(name: "Comics");
        migrationBuilder.DropTable(name: "Publishers");
        migrationBuilder.DropTable(name: "Genres");
        migrationBuilder.DropTable(name: "Themes");
    }
}
