# Quick Start Guide - Simple DOD Framework with DI

## ✅ Setup Complete!

The Simple DOD Framework is now fully integrated with dependency injection in your ComicApiDod project.

## What You Can Do Now

### 1. Use in Any HTTP Endpoint

Simply inject the services as parameters:

```csharp
app.MapGet("/your-endpoint", async (
    long id,
    SimpleMessageBusService messageBus,
    SimpleMapService mapService) =>
{
    var requestId = Guid.NewGuid().GetHashCode();
    
    // Enqueue your request
    messageBus.Enqueue(new YourRequest { RequestId = requestId, Id = id });
    
    // Wait for response
    var timeout = DateTime.UtcNow.AddSeconds(5);
    while (DateTime.UtcNow < timeout)
    {
        if (mapService.Find<YourResponse>(requestId, out var response))
        {
            mapService.Remove(requestId);
            return Results.Ok(response);
        }
        await Task.Delay(10);
    }
    
    return Results.StatusCode(408); // Timeout
});
```

### 2. Use in Any Service Class

Inject via constructor:

```csharp
public class YourService
{
    private readonly SimpleMessageBusService _messageBus;
    private readonly SimpleMapService _mapService;
    private readonly ILogger<YourService> _logger;
    
    public YourService(
        SimpleMessageBusService messageBus,
        SimpleMapService mapService,
        ILogger<YourService> logger)
    {
        _messageBus = messageBus;
        _mapService = mapService;
        _logger = logger;
    }
    
    public async Task<Response> ProcessAsync(Request request)
    {
        var requestId = Guid.NewGuid().GetHashCode();
        request.RequestId = requestId;
        
        _messageBus.Enqueue(request);
        
        // Poll for response with timeout
        var timeout = DateTime.UtcNow.AddSeconds(5);
        while (DateTime.UtcNow < timeout)
        {
            if (_mapService.Find<Response>(requestId, out var response))
            {
                _mapService.Remove(requestId);
                return response;
            }
            await Task.Delay(10);
        }
        
        throw new TimeoutException("Request processing timed out");
    }
}
```

### 3. Add Your Own Message Processor

Edit `Services/MessageProcessingHostedService.cs`:

```csharp
public Task StartAsync(CancellationToken cancellationToken)
{
    _logger.LogInformation("Message Processing Hosted Service is starting.");

    // 1. Register your queue
    _messageBusService.RegisterQueue<YourRequest>(new SimpleQueue<YourRequest>());
    
    // 2. Start your batch processor
    _processingTasks.Add(_messageBusService.StartBatchListener<YourRequest>(
        batchSize: 10,  // Process up to 10 messages at once
        callback: ProcessYourRequestBatch,
        cancellationToken: cancellationToken));

    _logger.LogInformation("Message Processing Hosted Service started successfully.");
    return Task.CompletedTask;
}

// 3. Implement your batch processor
private void ProcessYourRequestBatch(int batchSize, YourRequest?[] requests)
{
    _logger.LogDebug("Processing batch of {BatchSize} requests", batchSize);
    
    for (int i = 0; i < batchSize; i++)
    {
        if (requests[i] != null)
        {
            var request = requests[i]!;
            
            try
            {
                // YOUR BUSINESS LOGIC HERE
                // Example: Query database, calculate something, etc.
                var response = new YourResponse(request.RequestId)
                {
                    // Set response properties based on request
                    Result = request.Value * 2,
                    ProcessedAt = DateTime.UtcNow
                };
                
                // Store response so handler can retrieve it
                _mapService.Add(response);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error processing request {RequestId}", request.RequestId);
            }
        }
    }
}
```

## Your Request and Response Models

### Request Model (No special requirements)
```csharp
public class YourRequest
{
    public int RequestId { get; set; }  // Required for response matching
    public long Id { get; set; }
    public string? SomeData { get; set; }
    // ... your properties
}
```

### Response Model (Must implement IValue)
```csharp
public class YourResponse : IValue
{
    public int Id { get; set; }  // This is the RequestId
    public string? Result { get; set; }
    public DateTime ProcessedAt { get; set; }
    // ... your properties
    
    public YourResponse(int requestId)
    {
        Id = requestId;
    }
}
```

## Benefits of This Approach

✅ **Non-blocking**: Handler threads return immediately after enqueueing
✅ **Batch processing**: Process multiple requests efficiently
✅ **Thread-safe**: All operations are thread-safe
✅ **Testable**: Easy to mock services in unit tests
✅ **DOD principles**: Clean separation of data and behavior
✅ **Available everywhere**: DI makes services accessible throughout your app

## Testing

The framework includes comprehensive tests in `ComicApiTests/SimpleQueueTest.cs`:

```bash
cd /Users/pbasnal/PersonalRepos/dotnet-tut/ComicApiOop/ComicApiTests
dotnet test
```

## Example: Current Implementation

See the example endpoint in `ProgramDod.cs`:
- Endpoint: `/api/comics/{comicId}/async-visibility`
- Request model: `ComicRequest`
- Response model: `ComicResponse`
- Models defined in: `Models/RequestResponse.cs`

## Performance Tuning Parameters

| Parameter | Location | Default | Description |
|-----------|----------|---------|-------------|
| Batch Size | `StartBatchListener()` | 10 | Messages per batch |
| Poll Interval | Handler loop | 10ms | How often to check for response |
| Timeout | Handler loop | 5s | Max wait time for response |
| Queue Type | `RegisterQueue()` | `SimpleQueue<T>` | Queue implementation |

## Next Steps

1. **Define your models** in `Models/` folder
2. **Register queue** in `MessageProcessingHostedService.StartAsync()`
3. **Implement processor** in `MessageProcessingHostedService`
4. **Use in handlers** by injecting `SimpleMessageBusService` and `SimpleMapService`

## Verification

✅ Application starts successfully
✅ `MessageProcessingHostedService` logs show startup
✅ Services are available via DI
✅ Health endpoint works: `curl http://localhost:5000/health`
✅ Build succeeds with no errors

## Need Help?

- See `README.md` for detailed architecture explanation
- See `DEPENDENCY_INJECTION_SETUP.md` for complete setup documentation
- See `ComicApiTests/SimpleQueueTest.cs` for usage examples
- Check logs in console for `MessageProcessingHostedService` messages


