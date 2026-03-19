using ComicApiOop.Middleware;
using Common.Models;
using Microsoft.EntityFrameworkCore;
using Prometheus;

namespace ComicApiOop.Extensions;

public static class ApplicationBuilderExtensions
{
    public static IApplicationBuilder UseComicApiPipeline(this IApplicationBuilder app, IWebHostEnvironment environment)
    {
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

        // Stamp request time as early as possible for Request Wait Time metric
        app.UseMiddleware<RequestWaitTimeMiddleware>();

        // Add metrics endpoint and middleware
        app.UseMetricServer();

        // Add custom metrics middleware
        app.UseMiddleware<MetricsMiddleware>();

        return app;
    }
}
