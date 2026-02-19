using Common.Metrics;
using System.Diagnostics;

namespace ComicApiOop.Middleware;

public class MetricsMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<MetricsMiddleware> _logger;
    private readonly IAppMetrics _appMetrics;

    public MetricsMiddleware(RequestDelegate next, ILogger<MetricsMiddleware> logger, IAppMetrics appMetrics)
    {
        _next = next;
        _logger = logger;
        _appMetrics = appMetrics;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var sw = Stopwatch.StartNew();
        string status;

        try
        {
            await _next(context);
            status = context.Response.StatusCode.ToString();
        }
        catch
        {
            status = "500";
            _appMetrics.CaptureCount("oop_http_request", 1, new Dictionary<string, string> { ["status"] = status });
            _appMetrics.RecordLatency("oop_http_request", sw.Elapsed.TotalSeconds, new Dictionary<string, string> { ["status"] = status });
            throw;
        }

        _appMetrics.CaptureCount("oop_http_request", 1, new Dictionary<string, string> { ["status"] = status });
        _appMetrics.RecordLatency("oop_http_request", sw.Elapsed.TotalSeconds, new Dictionary<string, string> { ["status"] = status });
    }
}
