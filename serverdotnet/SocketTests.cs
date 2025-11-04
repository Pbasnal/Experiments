using System.Net;
using System.Net.Sockets;
using System.Text;
using Xunit;

public class SocketTests
{
    [Fact]
    public async Task Test_BasicEchoServer()
    {
        using var context = await SocketTestContext.Create()
            .WithServerSocket()
            .WithClientSocket()
            .ConnectAsync()
            .SendFromClient("Hello Server!")
            .VerifyServerReceived("Hello Server!")
            .SendFromServer("Hello Client!")
            .VerifyClientReceived("Hello Client!");

        Assert.True(context.IsValid);
    }

    [Fact]
    public async Task Test_MultipleClients()
    {
        using var context = await SocketTestContext.Create()
            .WithServerSocket()
            .WithClientCount(3)
            .ConnectAllAsync();

        foreach (var clientId in Enumerable.Range(0, 3))
        {
            await context
                .SendFromClient($"Hello from client {clientId}!", clientId)
                .VerifyServerReceived($"Hello from client {clientId}!", clientId)
                .SendFromServer($"Hello client {clientId}!", clientId)
                .VerifyClientReceived($"Hello client {clientId}!", clientId);
        }

        Assert.True(context.IsValid);
    }

    [Fact]
    public async Task Test_NonBlockingOperations()
    {
        using var context = await SocketTestContext.Create()
            .WithServerSocket()
            .ConfigureServer(socket => socket.Blocking = false)
            .VerifyNonBlockingBehavior();

        Assert.True(context.IsValid);
    }

    [Fact]
    public async Task Test_SocketOptions()
    {
        using var context = await SocketTestContext.Create()
            .WithServerSocket()
            .ConfigureServer(socket =>
            {
                socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
                socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.KeepAlive, true);
                socket.ReceiveBufferSize = 8192;
                socket.SendBufferSize = 8192;
            })
            .VerifySocketOptions();

        Assert.True(context.IsValid);
    }
}

public class SocketTestContext : IDisposable
{
    private Socket? _serverSocket;
    private List<Socket> _clientSockets = new();
    private List<Socket> _acceptedSockets = new();
    private bool _isValid = true;
    private readonly List<Task> _acceptTasks = new();
    private CancellationTokenSource _cts = new();

    public bool IsValid => _isValid;
    public int Port { get; private set; }

    public static SocketTestContext Create()
    {
        return new SocketTestContext();
    }

    public SocketTestContext WithServerSocket()
    {
        _serverSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        var endpoint = new IPEndPoint(IPAddress.Loopback, 0);
        _serverSocket.Bind(endpoint);
        _serverSocket.Listen(128);
        Port = ((IPEndPoint)_serverSocket.LocalEndPoint!).Port;
        return this;
    }

    public SocketTestContext WithClientSocket()
    {
        _clientSockets.Add(new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp));
        return this;
    }

    public SocketTestContext WithClientCount(int count)
    {
        for (int i = 0; i < count; i++)
        {
            WithClientSocket();
        }
        return this;
    }

    public SocketTestContext ConfigureServer(Action<Socket> configure)
    {
        if (_serverSocket == null)
            throw new InvalidOperationException("Server socket not initialized");

        configure(_serverSocket);
        return this;
    }

    public async Task<SocketTestContext> ConnectAsync()
    {
        if (_clientSockets.Count == 0)
            throw new InvalidOperationException("No client sockets created");

        // Start accepting clients
        _acceptTasks.Add(AcceptClientsAsync(_cts.Token));

        // Connect clients
        foreach (var client in _clientSockets)
        {
            await client.ConnectAsync(IPAddress.Loopback, Port);
        }

        // Wait for all accepts to complete
        await Task.Delay(100); // Give some time for accepts to complete
        return this;
    }

    public async Task<SocketTestContext> ConnectAllAsync()
    {
        return await ConnectAsync();
    }

    public async Task<SocketTestContext> SendFromClient(string message, int clientIndex = 0)
    {
        var data = Encoding.UTF8.GetBytes(message);
        await _clientSockets[clientIndex].SendAsync(data, SocketFlags.None);
        return this;
    }

    public async Task<SocketTestContext> SendFromServer(string message, int clientIndex = 0)
    {
        var data = Encoding.UTF8.GetBytes(message)`;
        await _acceptedSockets[clientIndex].SendAsync(data, SocketFlags.None);
        return this;
    }

    public async Task<SocketTestContext> VerifyServerReceived(string expectedMessage, int clientIndex = 0)
    {
        var buffer = new byte[1024];
        var received = await _acceptedSockets[clientIndex].ReceiveAsync(buffer, SocketFlags.None);
        var message = Encoding.UTF8.GetString(buffer, 0, received);
        _isValid &= message == expectedMessage;
        return this;
    }

    public async Task<SocketTestContext> VerifyClientReceived(string expectedMessage, int clientIndex = 0)
    {
        var buffer = new byte[1024];
        var received = await _clientSockets[clientIndex].ReceiveAsync(buffer, SocketFlags.None);
        var message = Encoding.UTF8.GetString(buffer, 0, received);
        _isValid &= message == expectedMessage;
        return this;
    }

    public SocketTestContext VerifySocketOptions()
    {
        if (_serverSocket == null)
            throw new InvalidOperationException("Server socket not initialized");

        _isValid &= _serverSocket.ReceiveBufferSize == 8192;
        _isValid &= _serverSocket.SendBufferSize == 8192;
        return this;
    }

    public async Task<SocketTestContext> VerifyNonBlockingBehavior()
    {
        if (_serverSocket == null)
            throw new InvalidOperationException("Server socket not initialized");

        try
        {
            var client = _serverSocket.Accept();
            _isValid = false; // Should not reach here
        }
        catch (SocketException ex)
        {
            _isValid &= (ex.SocketErrorCode == SocketError.WouldBlock || 
                        ex.SocketErrorCode == SocketError.TryAgain);
        }
        return this;
    }

    private async Task AcceptClientsAsync(CancellationToken cancellationToken)
    {
        if (_serverSocket == null)
            throw new InvalidOperationException("Server socket not initialized");

        while (!cancellationToken.IsCancellationRequested)
        {
            try
            {
                var acceptedSocket = await _serverSocket.AcceptAsync(cancellationToken);
                _acceptedSockets.Add(acceptedSocket);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }
    }

    public void Dispose()
    {
        _cts.Cancel();
        foreach (var task in _acceptTasks)
        {
            task.Wait(1000);
        }

        foreach (var socket in _clientSockets)
        {
            socket.Close();
        }

        foreach (var socket in _acceptedSockets)
        {
            socket.Close();
        }

        _serverSocket?.Close();
        _cts.Dispose();
    }
} 