using Common.SimpleDodFramework;
using Microsoft.Extensions.Logging;

namespace Common.SimpleQueue;

public class SimpleMessageBus
{
    private readonly Dictionary<Type, ISimpleQueue> _allQueues;
    private readonly Dictionary<Type, CancellationTokenSource> _activeListeners;
    private readonly ILogger<SimpleMessageBus> _logger;

    public SimpleMessageBus(ILogger<SimpleMessageBus> logger)
    {
        _logger = logger;
        _allQueues = new Dictionary<Type, ISimpleQueue>();
        _activeListeners = new Dictionary<Type, CancellationTokenSource>();
    }

    public void RegisterQueue<T>(ISimpleQueue queue)
    {
        if (_allQueues.ContainsKey(typeof(T))) return;
        _allQueues.Add(typeof(T), queue);
        _logger.LogInformation("Registered queue for type {TypeName}", typeof(T).Name);
    }

    public void Enqueue<T>(T message)
    {
        var messageType = typeof(T);
        if (!_allQueues.ContainsKey(messageType))
        {
            throw new Exception($"No queue registered for type {messageType.Name}");
        }

        if (_allQueues[messageType] is SimpleQueue<T> queue)
        {
            queue.Enqueue(message);
        }
    }

    public Task StartBatchListener<T>(int batchSize, Func<int, List<T?>, Task<IValue[]>> callback, CancellationToken cancellationToken = default)
    {
        var messageType = typeof(T);
        var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _activeListeners[messageType] = cts;

        _logger.LogInformation("Starting batch listener for type {TypeName} with batch size {BatchSize}",
            messageType.Name, batchSize);

        return Task.Run(async () =>
        {
            try
            {
                if (!_allQueues.ContainsKey(messageType))
                {
                    throw new Exception($"No queue registered for type {messageType.Name}");
                }

                if (_allQueues[messageType] is SimpleQueue<T> queue)
                {
                    await queue.BatchDequeue(batchSize, callback, cts.Token);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in batch listener for type {TypeName}", messageType.Name);
            }
        }, cts.Token);
    }

    public void StopBatchListener<T>()
    {
        var messageType = typeof(T);
        if (_activeListeners.TryGetValue(messageType, out var cts))
        {
            cts.Cancel();
            _activeListeners.Remove(messageType);
            _logger.LogInformation("Stopped batch listener for type {TypeName}", messageType.Name);
        }
    }

    public void StopAllListeners()
    {
        foreach (var cts in _activeListeners.Values)
        {
            cts.Cancel();
        }
        _activeListeners.Clear();
        _logger.LogInformation("Stopped all batch listeners");
    }
}