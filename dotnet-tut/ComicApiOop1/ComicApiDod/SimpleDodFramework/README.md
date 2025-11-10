# Simple DOD Framework - Dependency Injection Guide

This document explains how to use the Simple DOD Framework components with dependency injection in your ASP.NET Core application.

## Overview

The framework consists of three main components:

1. **SimpleQueue**: A thread-safe queue for asynchronous message processing
2. **SimpleMap**: A thread-safe map for storing responses
3. **SimpleMessageBus**: A message bus that coordinates queues and their processors

## Architecture

```
HTTP Request → Handler Thread → Enqueue Request → SimpleQueue
                                                      ↓
                                                Processor Thread (Background)
                                                      ↓
                                                Process Request
                                                      ↓
                                                Store Response → SimpleMap
                                                      ↓
Handler Thread ← Poll for Response ← SimpleMap
```

## Setup in ProgramDod.cs

The framework is already configured in `ProgramDod.cs`:

```csharp
// Register services as Singletons (they are thread-safe)
builder.Services.AddSingleton<SimpleMapService>();
builder.Services.AddSingleton<SimpleMessageBusService>();

// Add hosted service for background processing
builder.Services.AddHostedService<MessageProcessingHostedService>();
```

## How to Use

### 1. Define Your Request and Response Models

```csharp
// Request message
public class ComicRequest
{
    public int RequestId { get; set; }
    public long ComicId { get; set; }
    // ... other properties
}

// Response must implement IValue
public class ComicResponse : IValue
{
    public int Id { get; set; } // This is the RequestId
    public long ComicId { get; set; }
    public bool IsVisible { get; set; }
    // ... other properties
}
```

### 2. Register Queue and Start Processor

Edit `MessageProcessingHostedService.cs`:

```csharp
public Task StartAsync(CancellationToken cancellationToken)
{
    // Register the queue
    _messageBusService.RegisterQueue<ComicRequest>(new SimpleQueue<ComicRequest>());
    
    // Start the batch processor
    _processingTasks.Add(_messageBusService.StartBatchListener<ComicRequest>(
        batchSize: 10,
        callback: ProcessComicRequestBatch,
        cancellationToken: cancellationToken));
    
    return Task.CompletedTask;
}

private void ProcessComicRequestBatch(int batchSize, ComicRequest?[] requests)
{
    for (int i = 0; i < batchSize; i++)
    {
        if (requests[i] != null)
        {
            var request = requests[i]!;
            
            // Process the request (e.g., query database, apply business logic)
            var response = new ComicResponse(request.RequestId)
            {
                ComicId = request.ComicId,
                IsVisible = true, // Your logic here
                // ... set other properties
            };
            
            // Store the response
            _mapService.Add(response);
        }
    }
}
```

### 3. Use in Your HTTP Handlers

Inject the services into your endpoint:

```csharp
app.MapGet("/api/comics/{comicId}", async (
    long comicId,
    SimpleMessageBusService messageBus,
    SimpleMapService mapService) =>
{
    // 1. Generate unique request ID
    var requestId = Guid.NewGuid().GetHashCode();
    
    // 2. Create and enqueue request
    var request = new ComicRequest
    {
        RequestId = requestId,
        ComicId = comicId
    };
    messageBus.Enqueue(request);
    
    // 3. Poll for response
    var timeout = DateTime.UtcNow.AddSeconds(5);
    while (DateTime.UtcNow < timeout)
    {
        if (mapService.Find<ComicResponse>(requestId, out var response))
        {
            mapService.Remove(requestId); // Clean up
            return Results.Ok(response);
        }
        await Task.Delay(10); // Wait before next check
    }
    
    return Results.StatusCode(408); // Timeout
});
```

## Benefits

1. **Separation of Concerns**: Handler threads don't block on processing
2. **Batch Processing**: Process multiple requests together for efficiency
3. **Thread Safety**: All components are thread-safe
4. **Dependency Injection**: Easy to test and maintain
5. **DOD Principles**: Data and behavior are separated

## Testing

See `ComicApiTests/SimpleQueueTest.cs` for examples:

- `SimpleMapTest`: Basic map operations
- `SimpleRequestResponseFlowTest`: Complete request-response flow
- `BatchDequeue_EnqueueMessages_CallbackReceivesMessages`: Queue processing

## Performance Considerations

1. **Batch Size**: Larger batches = better throughput, but higher latency
2. **Poll Interval**: Lower interval = lower latency, but more CPU usage
3. **Timeout**: Balance between user experience and resource usage
4. **Memory**: Remember to remove responses from the map after use

## Example Use Cases

1. **Comic Visibility Computation**: Queue visibility checks, process in batches
2. **Price Calculations**: Queue price requests, calculate in bulk
3. **Content Filtering**: Queue filter requests, process efficiently
4. **Metrics Collection**: Queue metric events, batch write to storage


