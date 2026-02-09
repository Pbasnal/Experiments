using System.Collections.Concurrent;
using System.Diagnostics;
using Common.SimpleDodFramework;

namespace Common.SimpleQueue;

public interface ISimpleQueue
{
}

public class SimpleQueue<T> : ISimpleQueue
{
    private ConcurrentQueue<T> _queue = new ConcurrentQueue<T>();

    private long _dequeueTimeoutMs = 2000;

    private int _batchDequeueTimeoutMs = 10;

    public void Enqueue(T item)
    {
        _queue.Enqueue(item);
    }

    public List<T?> Dequeue(int batchSize)
    {
        List<T?> messageBatch = new List<T?>(batchSize);

        while (messageBatch.Count < batchSize && _queue.TryDequeue(out T? message))
        {
            messageBatch.Add(message);
        }

        return messageBatch;
    }

    public async Task<List<IValue>> BatchDequeue(
        int batchSize,
        Func<int, List<T?>, Task<IValue[]>> callback,
        CancellationToken cancellationToken = default)
    {
        if (batchSize <= 0) batchSize = 10;

        TimeSpan period = TimeSpan.FromMilliseconds(0);
        Stopwatch sw = Stopwatch.StartNew();
        long nextTick = 0;

        int numberOfEmptyDequeue = 0;


        while (!cancellationToken.IsCancellationRequested)
        {
            List<T?> messageBatch = Dequeue(batchSize);

            if (messageBatch.Count == 0)
            {
                numberOfEmptyDequeue++;
                if (numberOfEmptyDequeue > 5)
                {
                    period = TimeSpan.FromMilliseconds(10);
                }

                continue;
            }

            numberOfEmptyDequeue = 0;
            period = TimeSpan.FromMilliseconds(0);

            try
            {
                callback(messageBatch.Count, messageBatch);
            }
            catch (Exception)
            {
                continue;
            }

            nextTick += period.Ticks;
            long delay = nextTick - sw.Elapsed.Ticks;
            try
            {
                if (delay > 0)
                    await Task.Delay(TimeSpan.FromTicks(delay), cancellationToken);
                else
                    await Task.Yield();
                await Task.Delay(_batchDequeueTimeoutMs, cancellationToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }

        // Task[] remaining;
        // lock (inFlightLock)
        // {
        //     remaining = inFlightTasks.ToArray();
        // }
        //
        // if (remaining.Length > 0)
        //     await Task.WhenAll(remaining);

        return new List<IValue>();
    }
}