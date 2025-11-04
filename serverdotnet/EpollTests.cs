using System.Net;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using Microsoft.Win32.SafeHandles;
using Xunit;

public class EpollTests
{
    [Fact]
    public async Task TestEpoll()
    {
        if (!OperatingSystem.IsLinux())
            return;

        using var epollContext = EpollContext.Create()
            .CreateServerSocket()
            .RegisterWithEpoll()
            .WaitForEvents();

        Assert.True(epollContext.IsValid);
    }
}

public class EpollContext : IDisposable
{
    private SafeFileHandle? _epoll;
    private Socket? _serverSocket;
    private bool _isValid;

    private const int EPOLLIN = 0x1;
    private const int EPOLLOUT = 0x4;
    private const int EPOLL_CTL_ADD = 1;

    public bool IsValid => _isValid;

    public static EpollContext Create()
    {
        var context = new EpollContext();
        context._epoll = new SafeFileHandle(LibC.epoll_create1(0), true);
        context._isValid = !context._epoll.IsInvalid;
        return context;
    }

    public EpollContext CreateServerSocket()
    {
        _serverSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        var endpoint = new IPEndPoint(IPAddress.Loopback, 0);
        _serverSocket.Bind(endpoint);
        _serverSocket.Listen(128);
        return this;
    }

    public EpollContext RegisterWithEpoll()
    {
        if (_epoll == null || _serverSocket == null)
            throw new InvalidOperationException("Epoll or server socket not initialized");

        var ev = new epoll_event
        {
            events = EPOLLIN | EPOLLOUT,
            data = new epoll_data { fd = _serverSocket.Handle.ToInt32() }
        };

        var result = LibC.epoll_ctl(
            _epoll.DangerousGetHandle().ToInt32(),
            EPOLL_CTL_ADD,
            _serverSocket.Handle.ToInt32(),
            ref ev);

        _isValid &= (result == 0);
        return this;
    }

    public EpollContext WaitForEvents(int timeoutMs = 100)
    {
        if (_epoll == null)
            throw new InvalidOperationException("Epoll not initialized");

        var events = new epoll_event[10];
        var nfds = LibC.epoll_wait(
            _epoll.DangerousGetHandle().ToInt32(),
            events,
            events.Length,
            timeoutMs);

        _isValid &= (nfds >= 0);
        return this;
    }

    public void Dispose()
    {
        _serverSocket?.Close();
        _epoll?.Dispose();
    }

    private struct epoll_event
    {
        public int events;
        public epoll_data data;
    }

    private struct epoll_data
    {
        public int fd;
    }

    private static class LibC
    {
        [DllImport("libc", SetLastError = true)]
        public static extern int epoll_create1(int flags);

        [DllImport("libc", SetLastError = true)]
        public static extern int epoll_ctl(int epfd, int op, int fd, ref epoll_event ev);

        [DllImport("libc", SetLastError = true)]
        public static extern int epoll_wait(int epfd, [Out] epoll_event[] events, int maxevents, int timeout);
    }
} 