public class WebServer : IDisposable
{
    private readonly SocketListener _listener;
    private readonly RequestHandler _handler;
    private readonly CancellationTokenSource _cts;
    private readonly Task[] _handlerTasks;

    public WebServer(int port, int handlerCount = 4)
    {
        var requestQueue = new RequestQueue();
        var socketPool = new SocketPool(10000);
        
        _listener = new SocketListener(port, requestQueue);
        _handler = new RequestHandler(requestQueue, socketPool);
        _cts = new CancellationTokenSource();
        _handlerTasks = new Task[handlerCount];
    }

    public async Task StartAsync()
    {
        // Start request handlers
        for (int i = 0; i < _handlerTasks.Length; i++)
        {
            _handlerTasks[i] = _handler.ProcessRequestsAsync(_cts.Token);
        }

        // Start listener
        await _listener.StartAsync(_cts.Token);
    }

    public void Dispose()
    {
        _cts.Cancel();
        Task.WaitAll(_handlerTasks);
        _cts.Dispose();
    }
} 