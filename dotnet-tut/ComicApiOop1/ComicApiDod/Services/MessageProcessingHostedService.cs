using ComicApiDod.SimpleQueue;

namespace ComicApiDod.Services;

/// <summary>
/// Background service that initializes and manages message queue processing.
/// This service starts when the application starts and stops gracefully on shutdown.
/// </summary>
public class MessageProcessingHostedService : IHostedService
{
    private readonly SimpleMessageBus _messageBus;
    private readonly SimpleMap _map;
    private readonly ILogger<MessageProcessingHostedService> _logger;
    private readonly List<Task> _processingTasks;

    public MessageProcessingHostedService(
        SimpleMessageBus messageBus,
        SimpleMap map,
        ILogger<MessageProcessingHostedService> logger)
    {
        _messageBus = messageBus;
        _map = map;
        _logger = logger;
        _processingTasks = new List<Task>();
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Message Processing Hosted Service is starting.");

        // Register queues here for different message types
        // Example: _messageBus.RegisterQueue<YourMessageType>(new SimpleQueue<YourMessageType>());
        
        // Start batch listeners here
        // Example: _processingTasks.Add(_messageBus.StartBatchListener<YourMessageType>(
        //     batchSize: 10,
        //     callback: ProcessYourMessageBatch,
        //     cancellationToken: cancellationToken));

        _logger.LogInformation("Message Processing Hosted Service started successfully.");
        
        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Message Processing Hosted Service is stopping.");

        // Stop all listeners
        _messageBus.StopAllListeners();

        // Wait for all processing tasks to complete (with timeout)
        if (_processingTasks.Any())
        {
            var timeoutTask = Task.Delay(TimeSpan.FromSeconds(30), cancellationToken);
            var allTasks = Task.WhenAll(_processingTasks);
            
            await Task.WhenAny(allTasks, timeoutTask);
            
            if (!allTasks.IsCompleted)
            {
                _logger.LogWarning("Some processing tasks did not complete within the shutdown timeout.");
            }
        }

        _logger.LogInformation("Message Processing Hosted Service stopped.");
    }

    // Example callback method for processing messages
    // private void ProcessYourMessageBatch(int batchSize, YourMessageType?[] messages)
    // {
    //     _logger.LogDebug("Processing batch of {BatchSize} messages", batchSize);
    //     
    //     for (int i = 0; i < batchSize; i++)
    //     {
    //         if (messages[i] != null)
    //         {
    //             var message = messages[i]!;
    //             
    //             // Process the message
    //             var response = ProcessMessage(message);
    //             
    //             // Store the response in the map
    //             _mapService.Add(response);
    //         }
    //     }
    // }
}


