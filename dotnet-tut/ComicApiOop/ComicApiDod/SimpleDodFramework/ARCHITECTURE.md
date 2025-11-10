# Simple DOD Framework - Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        ASP.NET Core                              │
│                     Dependency Injection                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ├─ Singleton Services
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ SimpleMap    │    │ SimpleQueue  │    │ SimpleMsg    │
│ Service      │    │ (Internal)   │    │ BusService   │
└──────────────┘    └──────────────┘    └──────────────┘
        │                     │                     │
        └─────────────────────┴─────────────────────┘
                              │
                    Background Service
                              │
                              ▼
              ┌───────────────────────────┐
              │ MessageProcessing         │
              │ HostedService             │
              │ (IHostedService)          │
              └───────────────────────────┘
```

## Request-Response Flow

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│  Handler Thread (ASP.NET Request Handler)                    │
│                                                               │
│  1. Generate unique RequestId                                │
│  2. Create Request object                                    │
│  3. messageBus.Enqueue(request)  ──────────┐                │
│  4. Poll for response in loop              │                │
│     while (timeout not reached)            │                │
│       if mapService.Find(requestId)        │                │
│         return response ←──────────────────┼────────┐       │
│     return timeout                         │        │       │
└────────────────────────────────────────────┼────────┼───────┘
                                             │        │
                                             │        │
                                             ▼        │
                                    ┌─────────────────┼─────┐
                                    │  SimpleQueue    │     │
                                    │  (Thread-safe)  │     │
                                    └─────────────────┼─────┘
                                             │        │
                                             │        │
                                             ▼        │
┌─────────────────────────────────────────────────────┼───────┐
│  Processor Thread (Background Task)                 │       │
│                                                      │       │
│  1. BatchDequeue (batchSize: 10)                    │       │
│  2. Process batch:                                  │       │
│     for each message in batch                       │       │
│       - Execute business logic                      │       │
│       - Create response                             │       │
│       - mapService.Add(response) ───────────────────┘       │
│  3. Repeat continuously                                     │
└─────────────────────────────────────────────────────────────┘
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │  SimpleMap      │
                                    │  (Thread-safe)  │
                                    └─────────────────┘
```

## Component Details

### 1. SimpleQueue<T>
**Purpose**: Thread-safe message queue with batch dequeue capability

**Key Features**:
- `Enqueue(T item)`: Add message to queue
- `Dequeue()`: Remove single message (with timeout)
- `BatchDequeue(batchSize, callback)`: Dequeue up to `batchSize` messages and call callback

**Thread Safety**: Uses `ConcurrentQueue<T>` internally

**Location**: `SimpleDodFramework/SimpleQueue.cs`

### 2. SimpleMap
**Purpose**: Thread-safe storage for responses keyed by request ID

**Key Features**:
- `Find<T>(int id, out T? value)`: Retrieve value by ID
- `Add(IValue value)`: Store value (uses value.Id as key)
- `Remove(int id)`: Delete value
- `Count`: Number of items in map

**Thread Safety**: Uses `ConcurrentDictionary<int, IValue?>` internally

**Location**: `SimpleDodFramework/SimpleMap.cs`

**Constraint**: All stored values must implement `IValue` interface (provides `int Id` property)

### 3. SimpleMessageBus
**Purpose**: Coordinates multiple queues and routes messages

**Key Features**:
- `RegisterQueue<T>(ISimpleQueue queue)`: Register queue for type T
- `Enqueue<T>(T message)`: Route message to appropriate queue
- `BatchListenTo<T>(batchSize, callback)`: Start processing messages

**Thread Safety**: Uses `Dictionary<Type, ISimpleQueue>` with proper synchronization

**Location**: `SimpleDodFramework/SimpleMessageBus.cs`

### 4. SimpleMapService
**Purpose**: DI wrapper for SimpleMap

**Lifetime**: Singleton (registered once, shared across all requests)

**Thread Safety**: Inherits from SimpleMap (thread-safe)

**Location**: `SimpleDodFramework/SimpleMapService.cs`

**Usage**: Inject into constructors or endpoints

### 5. SimpleMessageBusService
**Purpose**: DI wrapper for SimpleMessageBus with lifecycle management

**Lifetime**: Singleton

**Additional Features**:
- Tracks active listeners
- Supports cancellation
- Logging integration
- Graceful shutdown

**Location**: `SimpleDodFramework/SimpleMessageBusService.cs`

### 6. MessageProcessingHostedService
**Purpose**: Background service that initializes and manages message processing

**Lifetime**: Hosted Service (starts with app, stops on shutdown)

**Responsibilities**:
- Register queues on startup
- Start batch listeners
- Manage processor tasks
- Graceful shutdown with timeout

**Location**: `Services/MessageProcessingHostedService.cs`

**Customization Point**: Add your message types and processors here

## Data Flow Example

### Step-by-Step: Comic Visibility Check

1. **HTTP Request arrives**
   ```
   GET /api/comics/12345/async-visibility?region=US&segment=premium
   ```

2. **Handler creates request**
   ```csharp
   var requestId = 1234567890; // from Guid.NewGuid().GetHashCode()
   var request = new ComicRequest
   {
       RequestId = requestId,
       ComicId = 12345,
       Region = "US",
       CustomerSegment = "premium"
   };
   ```

3. **Handler enqueues request**
   ```csharp
   messageBus.Enqueue(request);  // Thread-safe, non-blocking
   ```

4. **Background processor dequeues batch**
   ```csharp
   // Runs continuously in background
   // Gets up to 10 messages at once
   void ProcessBatch(int count, ComicRequest?[] requests)
   {
       // count = 3 (for example)
       // requests[0] = our request
       // requests[1] = another request
       // requests[2] = another request
   }
   ```

5. **Processor executes business logic**
   ```csharp
   // For each request in batch:
   var response = new ComicResponse(requestId)
   {
       ComicId = request.ComicId,
       IsVisible = true,  // Calculated
       CurrentPrice = 9.99m,  // Calculated
       ProcessedAt = DateTime.UtcNow
   };
   ```

6. **Processor stores response**
   ```csharp
   mapService.Add(response);  // Thread-safe
   ```

7. **Handler finds response**
   ```csharp
   // Handler has been polling in a loop
   if (mapService.Find<ComicResponse>(requestId, out var response))
   {
       mapService.Remove(requestId);  // Clean up
       return Results.Ok(response);
   }
   ```

8. **HTTP Response sent**
   ```json
   {
       "comicId": 12345,
       "isVisible": true,
       "currentPrice": 9.99,
       "processedAt": "2025-10-17T10:30:00Z"
   }
   ```

## Thread Safety Guarantees

### ✅ Safe Operations
- Multiple threads enqueueing simultaneously
- Multiple threads polling SimpleMap simultaneously
- Background processor running while handlers access map
- Multiple different message types being processed

### ⚠️ Important Notes
- Request IDs must be unique (use `Guid.NewGuid().GetHashCode()`)
- Always remove responses from map after reading (memory leak prevention)
- Poll interval affects both latency and CPU usage
- Batch size affects throughput and individual message latency

## Performance Characteristics

### Throughput
- **Batch Size 10**: ~1000 messages/sec (example)
- **Batch Size 100**: ~5000 messages/sec (example)
- Limited by your business logic processing time

### Latency
- **Best case**: 10ms (immediate dequeue + poll interval)
- **Average case**: 20-50ms (depends on batch position)
- **Worst case**: Timeout (5 seconds default)

### Memory
- Queue: O(n) where n = enqueued but not yet processed
- Map: O(m) where m = processed but not yet retrieved
- **Important**: Remove responses from map to prevent memory leaks

## Scaling Considerations

### Vertical Scaling
- Increase batch size for better throughput
- Add more processor threads for parallel processing
- Tune poll interval for latency vs CPU trade-off

### Horizontal Scaling
- Current implementation is in-memory (single instance)
- For multi-instance: Consider Redis or message queue
- Map storage would need distributed cache

## Testing Strategy

### Unit Tests
- Test SimpleQueue batch dequeue
- Test SimpleMap concurrent access
- Test message bus routing

### Integration Tests
- Test complete request-response flow
- Test timeout scenarios
- Test concurrent requests

**Test Location**: `ComicApiTests/SimpleQueueTest.cs`

## Error Handling

### Queue Errors
- Queue not registered: Exception thrown
- Dequeue timeout: Configurable per queue

### Map Errors
- Key not found: Returns false from Find()
- Null values: Supported (nullable IValue)

### Processing Errors
- Catch exceptions in batch processor
- Log errors but continue processing other messages
- Don't add response to map on error (handler will timeout)

## Monitoring Recommendations

Add Prometheus metrics for:
- Queue depth: `queue_depth{message_type="..."}`
- Processing time: `processing_duration_seconds{message_type="..."}`
- Timeout rate: `request_timeout_total{endpoint="..."}`
- Map size: `response_map_size`

## Extension Points

### Custom Queue Implementation
Implement `ISimpleQueue` for custom behavior:
- Priority queue
- Persistent queue
- Rate-limited queue

### Custom Storage
Replace SimpleMap with:
- Redis for distributed storage
- Time-based expiration
- Overflow to disk

### Custom Processors
Add domain-specific processors:
- Different batch sizes per message type
- Parallel processing within batch
- Retry logic for failures


