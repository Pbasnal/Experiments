using System.Collections.Concurrent;

namespace ComicApiDod.SimpleQueue;

public interface IValue
{
    public int Id { get; }
}

/// <summary>
/// Thread-safe storage for responses keyed by request ID.
/// This is designed to be registered as a singleton service.
/// </summary>
public class SimpleMap
{
    private readonly ConcurrentDictionary<int, IValue?> _map;
    private readonly ILogger<SimpleMap> _logger;

    public SimpleMap(ILogger<SimpleMap> logger)
    {
        _logger = logger;
        _map = new ConcurrentDictionary<int, IValue?>();
    }

    /// <summary>
    /// Find a value in the map by its ID.
    /// </summary>
    public bool Find<T>(int id, out T? value) where T : IValue
    {
        if (_map.TryGetValue(id, out IValue? valInMap))
        {
            value = (T)valInMap!;
            _logger.LogDebug("Found value of type {ValueType} for ID {Id}", typeof(T).Name, id);
            return true;
        }

        value = default;
        _logger.LogDebug("No value found for ID {Id}", id);
        return false;
    }

    /// <summary>
    /// Add a value to the map.
    /// </summary>
    public void Add(IValue value)
    {
        _map[value.Id] = value;
        _logger.LogDebug("Added value of type {ValueType} with ID {Id}", value.GetType().Name, value.Id);
    }

    /// <summary>
    /// Remove a value from the map by its ID.
    /// </summary>
    public bool Remove(int id)
    {
        var removed = _map.TryRemove(id, out _);
        if (removed)
        {
            _logger.LogDebug("Removed value with ID {Id}", id);
        }
        return removed;
    }

    /// <summary>
    /// Get the count of items in the map.
    /// </summary>
    public int Count => _map.Count;
}