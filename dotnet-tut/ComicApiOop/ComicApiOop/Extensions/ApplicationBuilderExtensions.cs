using ComicApiOop.Middleware;
using Common.Models;
using Microsoft.EntityFrameworkCore;
using Prometheus;

namespace ComicApiOop.Extensions;

public static class ApplicationBuilderExtensions
{
    public static IApplicationBuilder UseComicApiPipeline(this IApplicationBuilder app, IWebHostEnvironment environment)
    {
        // Apply database migrations in Docker environment
        if (environment.IsEnvironment("Docker"))
        {
            var loggerFactory = app.ApplicationServices.GetRequiredService<ILoggerFactory>();
            var logger = loggerFactory.CreateLogger("ComicApiOop.Extensions.ApplicationBuilderExtensions");
            
            logger.LogInformation("Running in Docker environment. Applying database migrations...");
            try
            {
                using (var scope = app.ApplicationServices.CreateScope())
                {
                    var db = scope.ServiceProvider.GetRequiredService<ComicDbContext>();
                    if (db.Database.GetPendingMigrations().Any())
                    {
                        logger.LogInformation("Applying pending migrations...");
                        db.Database.Migrate();
                        logger.LogInformation("Migrations applied successfully.");
                    }
                    else
                    {
                        logger.LogInformation("No pending migrations. Database is up to date.");
                    }
                }
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "An error occurred while migrating the database.");
                throw;
            }
        }

        // Configure the HTTP request pipeline
        if (environment.IsDevelopment())
        {
            app.UseSwagger();
            app.UseSwaggerUI();
        }

        // Only use HTTPS redirection in development (not in Docker)
        if (!environment.IsEnvironment("Docker"))
        {
            app.UseHttpsRedirection();
        }

        // Add metrics endpoint and middleware
        app.UseMetricServer();
        app.UseHttpMetrics();

        // Add custom metrics middleware
        app.UseMiddleware<MetricsMiddleware>();

        return app;
    }
}
