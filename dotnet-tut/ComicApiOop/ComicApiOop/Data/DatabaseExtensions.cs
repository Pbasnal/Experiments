using Common.Models;
using Microsoft.EntityFrameworkCore;

namespace ComicApiOop.Data;

public static class DatabaseExtensions
{
    public static void MigrateDatabase(this WebApplication app)
    {
        using (var scope = app.Services.CreateScope())
        {
            var db = scope.ServiceProvider.GetRequiredService<ComicDbContext>();
            
            // Retry pattern for database operations
            var retryCount = 0;
            const int maxRetries = 3;
            
            while (retryCount < maxRetries)
            {
                try
                {
                    app.Logger.LogInformation("Attempting to migrate database...");
                    db.Database.Migrate();
                    app.Logger.LogInformation("Database migration completed successfully.");
                    break;
                }
                catch (Exception ex)
                {
                    retryCount++;
                    app.Logger.LogError(ex, "Error migrating database. Attempt {RetryCount} of {MaxRetries}", retryCount, maxRetries);
                    
                    if (retryCount == maxRetries)
                        throw;
                    
                    // Wait before the next retry, with exponential backoff
                    Thread.Sleep(1000 * retryCount * retryCount);
                }
            }
        }
    }
}








