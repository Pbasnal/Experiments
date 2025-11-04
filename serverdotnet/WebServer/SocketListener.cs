using System.Net;
using System.Net.Sockets;

public class SocketListener
{
    private readonly Socket _listenerSocket;
    private readonly RequestQueue _requestQueue;
    private readonly SocketPool _socketPool;
    private readonly byte[][] _receiveBufferPool;
    private const int BufferSize = 8192;
    private const int MaxConcurrentRequests = 10000;

    public SocketListener(int port, RequestQueue requestQueue)
    {
        _listenerSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        _listenerSocket.Bind(new IPEndPoint(IPAddress.Any, port));
        _listenerSocket.Listen(MaxConcurrentRequests);
        
        _requestQueue = requestQueue;
        _socketPool = new SocketPool(MaxConcurrentRequests);
        _receiveBufferPool = new byte[MaxConcurrentRequests][];
        
        // Pre-allocate receive buffers
        for (int i = 0; i < MaxConcurrentRequests; i++)
        {
            _receiveBufferPool[i] = new byte[BufferSize];
        }
    }

    public async Task StartAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            var clientSocket = await _listenerSocket.AcceptAsync(cancellationToken);
            var socketId = _socketPool.Acquire(clientSocket);
            
            if (socketId >= 0)
            {
                _ = ProcessClientAsync(socketId, cancellationToken);
            }
            else
            {
                clientSocket.Close();
            }
        }
    }

    private async Task ProcessClientAsync(int socketId, CancellationToken cancellationToken)
    {
        try
        {
            var socket = _socketPool.GetSocket(socketId);
            var buffer = _receiveBufferPool[socketId];
            var received = await socket.ReceiveAsync(buffer, SocketFlags.None, cancellationToken);
            
            if (received > 0)
            {
                var request = new HttpRequest
                {
                    SocketId = socketId,
                    Data = buffer.AsMemory(0, received),
                    Timestamp = DateTime.UtcNow.Ticks
                };
                
                _requestQueue.Enqueue(request);
            }
            else
            {
                _socketPool.Release(socketId);
            }
        }
        catch
        {
            _socketPool.Release(socketId);
        }
    }
} 