using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ComicApiDod.Migrations
{
    /// <inheritdoc />
    public partial class correctingcolumnname : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.RenameColumn(
                name: "TagId",
                table: "ComicTags",
                newName: "TagsId");

            migrationBuilder.RenameColumn(
                name: "ComicId",
                table: "ComicTags",
                newName: "ComicsId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.RenameColumn(
                name: "TagsId",
                table: "ComicTags",
                newName: "TagId");

            migrationBuilder.RenameColumn(
                name: "ComicsId",
                table: "ComicTags",
                newName: "ComicId");
        }
    }
}
