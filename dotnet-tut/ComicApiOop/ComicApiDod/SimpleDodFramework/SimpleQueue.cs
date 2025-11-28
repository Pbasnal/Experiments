using System.Collections.Concurrent;
using System.Diagnostics;

namespace ComicApiDod.SimpleQueue;

public interface ISimpleQueue
{
}

public class SimpleQueue<T> : ISimpleQueue
{
    private ConcurrentQueue<T> _queue = new ConcurrentQueue<T>();

    private long _dequeueTimeoutMs = 2000;

    private int _batchDequeueTimeoutMs = 10;

    private SimpleMap SimpleMap;

    public SimpleQueue(SimpleMap simpleMap)
    {
        this.SimpleMap = simpleMap;
    }

    public void Enqueue(T item)
    {
        _queue.Enqueue(item);
    }

    public T? Dequeue()
    {
        long startTimer = System.DateTime.Now.Millisecond;
        long timeElapsed = 0;
        bool messageDequeued = false;
        T? message;
        do
        {
            messageDequeued = _queue.TryDequeue(out message);
            timeElapsed = DateTime.Now.Millisecond - startTimer;
        } while (!messageDequeued && timeElapsed < _dequeueTimeoutMs);

        if (!messageDequeued)
        {
            throw new TimeoutException($"Failed to dequeue message in the given timeout of {_dequeueTimeoutMs}");
        }

        return message;
    }

    public async Task<List<IValue>> BatchDequeue(int batchSize, Func<int, List<T?>, Task<IValue[]>> callback)
    {
        if (batchSize <= 0) batchSize = 10;

        var period = TimeSpan.FromMilliseconds(10);
        Stopwatch sw = Stopwatch.StartNew();

        long nextTick = 0;

        List<T?> messageBatch = new List<T?>(batchSize);

        while (true)
        {
            int numberOfDequeuedMsgs = 0;

            // real production - this will be a problem that can lead to message loss
            // if the service goes down after dequeue but before processing the message.
            while (numberOfDequeuedMsgs < batchSize && _queue.Count > 0)
            {
                messageBatch.Add(Dequeue());
            }

            if (messageBatch.Count > 0)
            {
                IValue[] responses = await callback(numberOfDequeuedMsgs, messageBatch);
                for (int i = 0; i < responses.Length; i++)
                {
                    if (responses[i] != null)
                    {
                        SimpleMap.Add(responses[i]);
                    }
                }
                messageBatch.Clear();
            }

            nextTick += period.Ticks;
            var delay = nextTick - sw.Elapsed.Ticks;
            if (delay > 0)
                await Task.Delay(TimeSpan.FromTicks(delay));
            else
                await Task.Yield(); // We're running behind, skip sleep

            await Task.Delay(_batchDequeueTimeoutMs);
        }
    }
}