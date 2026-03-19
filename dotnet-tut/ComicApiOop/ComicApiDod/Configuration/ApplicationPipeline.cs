using ComicApiDod.Middleware;
using Common.Metrics;
using Microsoft.Extensions.DependencyInjection;
using Prometheus;
using System.Diagnostics;

namespace ComicApiDod.Configuration;

public static class ApplicationPipeline
{
    public static void ConfigurePipeline(WebApplication app)
    {
        if (app.Environment.IsDevelopment())
        {
            app.UseSwagger();
            app.UseSwaggerUI();
        }

        app.UseMetricServer();
        app.UseHttpMetrics();

        // Stamp request time first (same as OOP API for comparable Request Wait Time)
        app.UseMiddleware<RequestWaitTimeMiddleware>();

        app.Use(HandleMetricsMiddleware);
    }

    private static async Task HandleMetricsMiddleware(HttpContext context, Func<Task> next)
    {
        var metrics = context.RequestServices.GetRequiredService<IAppMetrics>();
        var path = context.Request.Path.Value ?? "/";
        var method = context.Request.Method;
        var sw = Stopwatch.StartNew();
        var status = "500";

        try
        {
            await next();
            status = context.Response.StatusCode.ToString();
        }
        catch
        {
            status = "500";
            throw;
        }
        finally
        {
            var labels = new Dictionary<string, string>
            {
                ["status"] = status,
                ["endpoint"] = path,
                ["method"] = method
            };

            metrics.Inc(MetricNames.ApiHttpRequestsTotal, 1, labels);
            metrics.Observe(MetricNames.HttpRequestDuration, sw.Elapsed.TotalSeconds, labels);
        }
    }
}

