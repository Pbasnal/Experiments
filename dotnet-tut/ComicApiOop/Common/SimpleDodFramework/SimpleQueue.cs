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

    public void Enqueue(T item)
    {
        _queue.Enqueue(item);
    }

    public List<T?> Dequeue(int batchSize)
    {
        List<T?> messageBatch = new List<T?>(batchSize);

        long maxBatchingTime = 100;
        Stopwatch sw = new Stopwatch();
        sw.Start();
        while (messageBatch.Count < batchSize)
        {
            if (_queue.TryDequeue(out T? message))
            {
                messageBatch.Add(message);
            }
            else if (maxBatchingTime <= sw.ElapsedMilliseconds)
            {
                break;
            }
        }

        return messageBatch;
    }

    public async Task<List<IValue>> BatchDequeue(
        int batchSize,
        Func<int, List<T?>, Task<IValue[]>> callback,
        CancellationToken cancellationToken = default)
    {
        if (batchSize <= 0) batchSize = 50;

        TimeSpan queueIterationDelay = TimeSpan.FromMilliseconds(0);

        int numberOfEmptyDequeue = 0;


        while (!cancellationToken.IsCancellationRequested)
        {
            List<T?> messageBatch = Dequeue(batchSize);

            if (messageBatch.Count == 0)
            {
                numberOfEmptyDequeue++;
                if (numberOfEmptyDequeue > 5)
                    queueIterationDelay = TimeSpan.FromMilliseconds(2);
            }
            else
            {
                numberOfEmptyDequeue = 0;
                queueIterationDelay = TimeSpan.FromMilliseconds(0);
                try
                {
                    callback(messageBatch.Count, messageBatch);
                }
                catch (Exception)
                {
                }
            }
            await Task.Delay(queueIterationDelay, cancellationToken);
        }

        return new List<IValue>();
    }
}