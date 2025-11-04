public class RequestQueue
{
    private const int QueueSize = 10000;
    private readonly HttpRequest[] _requests = new HttpRequest[QueueSize];
    private volatile int _head;
    private volatile int _tail;
    private readonly object _lockObject = new();

    public void Enqueue(HttpRequest request)
    {
        lock (_lockObject)
        {
            var nextTail = (_tail + 1) % QueueSize;
            if (nextTail != _head)
            {
                _requests[_tail] = request;
                _tail = nextTail;
            }
        }
    }

    public bool TryDequeue(out HttpRequest request)
    {
        lock (_lockObject)
        {
            if (_head != _tail)
            {
                request = _requests[_head];
                _head = (_head + 1) % QueueSize;
                return true;
            }
        }
        request = default;
        return false;
    }
} 