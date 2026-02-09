using ComicApiOop.Services;
using Common.Models;
using Microsoft.EntityFrameworkCore;

namespace ComicApiOop.Extensions;

public static class ServiceCollectionExtensions
{
    public static IServiceCollection AddComicApiServices(this IServiceCollection services, IConfiguration configuration)
    {
        // Required for Request Wait Time metric (service reads request timestamp from HttpContext)
        services.AddHttpContextAccessor();

        // Add API Explorer and Swagger
        services.AddEndpointsApiExplorer();
        services.AddSwaggerGen();

        // Configure JSON serialization to handle circular references
        services.ConfigureHttpJsonOptions(options =>
        {
            options.SerializerOptions.ReferenceHandler = System.Text.Json.Serialization.ReferenceHandler.IgnoreCycles;
            options.SerializerOptions.WriteIndented = true;
        });

        // Add database context
        var connectionString = configuration.GetConnectionString("DefaultConnection");
        services.AddDbContext<ComicDbContext>(options =>
        {
            var serverVersion = new MySqlServerVersion(new Version(8, 0, 0));
            options.UseMySql(connectionString, serverVersion,
                mysqlOptions =>
                {
                    mysqlOptions.EnableRetryOnFailure(
                        maxRetryCount: 5,
                        maxRetryDelay: TimeSpan.FromSeconds(30),
                        errorNumbersToAdd: null);
                });
        });

        // Register MetricsReporter (scoped to match DbContext lifetime)
        services.AddScoped<MetricsReporter>(sp =>
        {
            var dbContext = sp.GetRequiredService<ComicDbContext>();
            return new MetricsReporter(dbContext);
        });

        return services;
    }
}
