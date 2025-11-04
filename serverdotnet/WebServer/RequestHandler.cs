using System.Net;
using System.Net.Sockets;
using System.Text;

public class RequestHandler
{
    private readonly RequestQueue _requestQueue;
    private readonly SocketPool _socketPool;
    private readonly byte[] _responseBuffer;

    public RequestHandler(RequestQueue requestQueue, SocketPool socketPool)
    {
        _requestQueue = requestQueue;
        _socketPool = socketPool;
        _responseBuffer = Encoding.ASCII.GetBytes(
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/plain\r\n" +
            "Connection: close\r\n" +
            "\r\n" +
            "Hello, World!");
    }

    public async Task ProcessRequestsAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            if (_requestQueue.TryDequeue(out var request))
            {
                try
                {
                    var socket = _socketPool.GetSocket(request.SocketId);
                    await socket.SendAsync(_responseBuffer, SocketFlags.None);
                }
                finally
                {
                    _socketPool.Release(request.SocketId);
                }
            }
            else
            {
                await Task.Delay(1, cancellationToken);
            }
        }
    }
} 