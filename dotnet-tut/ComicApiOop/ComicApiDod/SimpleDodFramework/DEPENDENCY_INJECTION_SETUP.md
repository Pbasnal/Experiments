# Dependency Injection Setup - Complete

## ✅ What Has Been Configured

The Simple DOD Framework (SimpleQueue, SimpleMap, SimpleMessageBus) has been fully integrated into the ComicApiDod project using ASP.NET Core's dependency injection system.

## Components Created

### 1. Core Framework Files
- ✅ `SimpleQueue.cs` - Thread-safe queue for messages
- ✅ `SimpleMap.cs` - Thread-safe map for storing responses
- ✅ `SimpleMessageBus.cs` - Message bus coordinator

### 2. DI Service Wrappers
- ✅ `SimpleMapService.cs` - Singleton service wrapper for SimpleMap
- ✅ `SimpleMessageBusService.cs` - Singleton service wrapper for SimpleMessageBus
- ✅ `MessageProcessingHostedService.cs` - Background service for message processing

### 3. Models
- ✅ `RequestResponse.cs` - Example request/response models (ComicRequest, ComicResponse, ComicResponseDto)

### 4. Registration in ProgramDod.cs
```csharp
// Services registered as Singletons (thread-safe)
builder.Services.AddSingleton<SimpleMapService>();
builder.Services.AddSingleton<SimpleMessageBusService>();

// Background processing service
builder.Services.AddHostedService<MessageProcessingHostedService>();
```

### 5. Example Endpoint
- ✅ `/api/comics/{comicId}/async-visibility` - Demonstrates request-response flow

## How It Works

### Request Flow
```
1. HTTP Handler receives request
   ↓
2. Injects SimpleMessageBusService and SimpleMapService
   ↓
3. Creates request with unique ID
   ↓
4. Enqueues request using messageBus.Enqueue()
   ↓
5. Polls mapService for response using Find()
   ↓
6. Returns response or timeout
```

### Processing Flow (Background)
```
1. MessageProcessingHostedService starts on app startup
   ↓
2. Registers queues for message types
   ↓
3. Starts batch listeners in background tasks
   ↓
4. BatchListener dequeues messages in batches (e.g., 10 at a time)
   ↓
5. Processes batch (your business logic)
   ↓
6. Stores responses in SimpleMap using Add()
```

## Usage in Your Code

### Inject Services into Endpoints
```csharp
app.MapGet("/your-endpoint", async (
    SimpleMessageBusService messageBus,
    SimpleMapService mapService) =>
{
    // Your handler code here
});
```

### Inject Services into Your Services
```csharp
public class YourService
{
    private readonly SimpleMessageBusService _messageBus;
    private readonly SimpleMapService _mapService;
    
    public YourService(
        SimpleMessageBusService messageBus,
        SimpleMapService mapService)
    {
        _messageBus = messageBus;
        _mapService = mapService;
    }
    
    public async Task<Result> ProcessAsync(Request request)
    {
        var requestId = Guid.NewGuid().GetHashCode();
        request.RequestId = requestId;
        
        _messageBus.Enqueue(request);
        
        // Poll for response
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
        
        throw new TimeoutException();
    }
}
```

## Next Steps to Use the Framework

### 1. Define Your Message Types
Create your request and response classes. Response must implement `IValue`:

```csharp
public class YourRequest
{
    public int RequestId { get; set; }
    // ... your properties
}

public class YourResponse : IValue
{
    public int Id { get; set; } // RequestId
    // ... your properties
}
```

### 2. Register Queue and Processor
Edit `MessageProcessingHostedService.cs` in the `StartAsync` method:

```csharp
public Task StartAsync(CancellationToken cancellationToken)
{
    // Register your queue
    _messageBusService.RegisterQueue<YourRequest>(new SimpleQueue<YourRequest>());
    
    // Start your processor
    _processingTasks.Add(_messageBusService.StartBatchListener<YourRequest>(
        batchSize: 10,
        callback: ProcessYourRequestBatch,
        cancellationToken: cancellationToken));
    
    return Task.CompletedTask;
}

private void ProcessYourRequestBatch(int batchSize, YourRequest?[] requests)
{
    for (int i = 0; i < batchSize; i++)
    {
        if (requests[i] != null)
        {
            var request = requests[i]!;
            
            // Your processing logic here
            var response = new YourResponse(request.RequestId)
            {
                // Set your response properties
            };
            
            _mapService.Add(response);
        }
    }
}
```

### 3. Use in Your Handlers
The services are now available everywhere via dependency injection!

## Benefits

✅ **Thread-Safe**: All components handle concurrent access safely
✅ **Batch Processing**: Process multiple requests together for efficiency
✅ **Non-Blocking**: Handler threads don't wait for processing
✅ **Testable**: Easy to inject mocks for testing
✅ **DOD Principles**: Data and behavior properly separated
✅ **Scalable**: Background processing can be tuned independently

## Performance Tuning

- **Batch Size**: Adjust in `StartBatchListener` (larger = better throughput)
- **Poll Interval**: Adjust `Task.Delay` in handler (lower = lower latency)
- **Timeout**: Adjust timeout duration based on processing needs
- **Queue Count**: Can register multiple queues for different message types

## Testing

See `ComicApiTests/SimpleQueueTest.cs` for comprehensive examples:
- Unit tests for SimpleMap
- Integration test for request-response flow
- Batch processing test

## Monitoring

You can add Prometheus metrics to track:
- Queue depth
- Processing time per batch
- Response wait time
- Timeout rate

## Documentation

- `README.md` - Framework overview and usage guide
- `DEPENDENCY_INJECTION_SETUP.md` - This file
- Code comments in all service files


