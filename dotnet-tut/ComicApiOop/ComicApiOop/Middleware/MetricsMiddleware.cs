using ComicApiOop.Metrics;
using System.Diagnostics;

namespace ComicApiOop.Middleware;

public class MetricsMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<MetricsMiddleware> _logger;

    public MetricsMiddleware(RequestDelegate next, ILogger<MetricsMiddleware> logger)
    {
        _next = next;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var path = context.Request.Path.Value;
        var method = context.Request.Method;
        var endpoint = $"{method} {path}";

        var sw = Stopwatch.StartNew();

        try
        {
            await _next(context);

            // Record metrics after successful request
            var status = context.Response.StatusCode.ToString();
            MetricsConfiguration.HttpRequestCounter
                .WithLabels(method, path, status)
                .Inc();

            MetricsConfiguration.HttpRequestDuration
                .WithLabels(method, path, status)
                .Observe(sw.Elapsed.TotalSeconds);
        }
        catch
        {
            // Record metrics after failed request
            MetricsConfiguration.HttpRequestCounter
                .WithLabels(method, path, "500")
                .Inc();

            MetricsConfiguration.HttpRequestDuration
                .WithLabels(method, path, "500")
                .Observe(sw.Elapsed.TotalSeconds);
            throw;
        }
    }
}
