namespace ComicApiOop.Middleware;

/// <summary>
/// Records when the request was first received so that "Request Wait Time" can be
/// measured in the service (time from request arrival to start of processing).
/// Must run early in the pipeline, before any other request-handling middleware.
/// </summary>
public class RequestWaitTimeMiddleware
{
    public const string RequestReceivedAtUtcKey = "RequestReceivedAtUtc";

    private readonly RequestDelegate _next;

    public RequestWaitTimeMiddleware(RequestDelegate next)
    {
        _next = next;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        context.Items[RequestReceivedAtUtcKey] = DateTime.UtcNow;
        await _next(context);
    }
}
