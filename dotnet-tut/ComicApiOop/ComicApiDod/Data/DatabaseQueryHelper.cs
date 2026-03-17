using System.Data;
using System.Data.Common;
using System.Text;
using Common.Models;
using Microsoft.EntityFrameworkCore;
using MySqlConnector;

namespace ComicApiDod.Data;

/// <summary>
/// Helper class for batch database queries in the Data-Oriented Design approach
/// Provides methods to fetch data in batches for efficient processing
/// </summary>
public static class DatabaseQueryHelper
{
    /// <summary>
    /// Bulk save all visibilities using batched INSERT ... VALUES (...),(...).
    /// 1000 rows per batch, processed in parallel. No AllowLoadLocalInfile required.
    /// </summary>
    /// <param name="dbFactory">DbContext factory to obtain connections from the pool</param>
    /// <param name="allVisibilities">All computed visibilities from batch</param>
    public static async Task SaveComputedVisibilitiesBulkAsync(IDbContextFactory<ComicDbContext> dbFactory,
        ComputedVisibilityData[] allVisibilities)
    {
        if (allVisibilities.Length == 0)
            return;
        const int batchSize = 1000;
        var tasks = new List<Task>();
        for (int i = 0; i < allVisibilities.Length; i += batchSize)
        {
            int start = i;
            int count = Math.Min(batchSize, allVisibilities.Length - i);
            tasks.Add(ExecuteBatchWithDbContextAsync(dbFactory, allVisibilities, start, count));
        }

        await Task.WhenAll(tasks);
    }

    private static async Task ExecuteBatchWithDbContextAsync(IDbContextFactory<ComicDbContext> dbFactory,
        ComputedVisibilityData[] allVisibilities, int start, int count)
    {
        await using var db = await dbFactory.CreateDbContextAsync();
        DbConnection connection = db.Database.GetDbConnection();
        if (connection.State != ConnectionState.Open)
            await connection.OpenAsync();

        await ExecuteBatchedInsertAsync(connection as MySqlConnection, allVisibilities, start, count);
    }

    private static async Task ExecuteBatchedInsertAsync(MySqlConnection conn, ComputedVisibilityData[] allVisibilities, int start, int count)
    {
        if (count == 0)
            return;

        const string columns = "ComicId,CountryCode,CustomerSegmentId,FreeChaptersCount,LastChapterReleaseTime," +
            "GenreId,PublisherId,AverageRating,SearchTags,IsVisible,ComputedAt,LicenseType,CurrentPrice," +
            "IsFreeContent,IsPremiumContent,AgeRating,ContentFlags,ContentWarning";

        var valuesSb = new StringBuilder();
        var cmd = new MySqlCommand { Connection = conn };
        int paramIndex = 0;

        for (int row = 0; row < count; row++)
        {
            if (row > 0)
                valuesSb.Append(",\n");
            valuesSb.Append('(');
            for (int col = 0; col < 18; col++)
            {
                if (col > 0)
                    valuesSb.Append(',');
                valuesSb.Append("@p").Append(paramIndex++);
            }
            valuesSb.Append(')');
        }

        cmd.CommandText = $"INSERT INTO ComputedVisibilities ({columns}) VALUES {valuesSb}";

        paramIndex = 0;
        for (int row = 0; row < count; row++)
        {
            var cv = allVisibilities[start + row];
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.ComicId);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.CountryCode ?? string.Empty);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.CustomerSegmentId);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.FreeChaptersCount);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.LastChapterReleaseTime);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.GenreId);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.PublisherId);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.AverageRating);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.SearchTags ?? string.Empty);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.IsVisible);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.ComputedAt);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, (int)cv.LicenseType);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.CurrentPrice);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.IsFreeContent);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.IsPremiumContent);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, (int)cv.AgeRating);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, (int)cv.ContentFlags);
            cmd.Parameters.AddWithValue("@p" + paramIndex++, cv.ContentWarning ?? string.Empty);
        }

        await cmd.ExecuteNonQueryAsync();
    }
}