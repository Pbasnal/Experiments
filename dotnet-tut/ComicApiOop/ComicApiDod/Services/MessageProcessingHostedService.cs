using ComicApiDod.Models;
using ComicApiDod.SimpleQueue;

namespace ComicApiDod.Services;

/// <summary>
/// Background service that initializes and manages message queue processing.
/// This service starts when the application starts and stops gracefully on shutdown.
/// </summary>
public class MessageProcessingHostedService : IHostedService
{
    private readonly SimpleMessageBus _messageBus;
    private readonly ComicVisibilityService _comicVisibilityService;
    private readonly ILogger<MessageProcessingHostedService> _logger;
    private readonly List<Task> _processingTasks;

    public MessageProcessingHostedService(
        SimpleMessageBus messageBus,
        ComicVisibilityService comicVisibilityService,
        ILogger<MessageProcessingHostedService> logger)
    {
        _messageBus = messageBus;
        _comicVisibilityService = comicVisibilityService;
        _logger = logger;
        _processingTasks = new List<Task>();
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Message Processing Hosted Service is starting.");

        _messageBus.RegisterQueue<VisibilityComputationRequest>(new SimpleQueue<VisibilityComputationRequest>());
        _processingTasks.Add(_messageBus.StartBatchListener<VisibilityComputationRequest>(
             batchSize: 10,
             callback: _comicVisibilityService.ComputeVisibilities,
             cancellationToken: cancellationToken));

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
}


