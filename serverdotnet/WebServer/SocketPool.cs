public class SocketPool
{
    private readonly Socket?[] _sockets;
    private readonly bool[] _inUse;
    private readonly object _lockObject = new();

    public SocketPool(int capacity)
    {
        _sockets = new Socket[capacity];
        _inUse = new bool[capacity];
    }

    public int Acquire(Socket socket)
    {
        lock (_lockObject)
        {
            for (int i = 0; i < _sockets.Length; i++)
            {
                if (!_inUse[i])
                {
                    _sockets[i] = socket;
                    _inUse[i] = true;
                    return i;
                }
            }
        }
        return -1;
    }

    public void Release(int socketId)
    {
        lock (_lockObject)
        {
            if (socketId >= 0 && socketId < _sockets.Length)
            {
                _sockets[socketId]?.Close();
                _sockets[socketId] = null;
                _inUse[socketId] = false;
            }
        }
    }

    public Socket GetSocket(int socketId)
    {
        return _sockets[socketId] ?? throw new InvalidOperationException("Socket not found");
    }
} 