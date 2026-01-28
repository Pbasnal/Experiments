using Common.Models;
using Microsoft.EntityFrameworkCore;

namespace ComicApiOop.Data.Migrations;

public static class MigrationManager
{
    public static IHost MigrateDatabase(this IHost host)
    {
        using (var scope = host.Services.CreateScope())
        {
            using (var appContext = scope.ServiceProvider.GetRequiredService<ComicDbContext>())
            {
                try
                {
                    var logger = scope.ServiceProvider.GetRequiredService<ILogger<ComicDbContext>>();
                    
                    if (appContext.Database.GetPendingMigrations().Any())
                    {
                        logger.LogInformation("Starting database migration...");
                        appContext.Database.Migrate();
                        logger.LogInformation("Database migration completed successfully.");
                    }
                    else
                    {
                        logger.LogInformation("No pending migrations. Database is up to date.");
                    }
                }
                catch (Exception ex)
                {
                    throw new Exception("An error occurred while migrating the database.", ex);
                }
            }
        }
        return host;
    }
}
