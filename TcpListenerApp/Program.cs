using System.Net;
using System.Net.Sockets;

namespace TcpListenerApp;

public class Program
{
  public static void Main(string[] args)
  {

  }

}

public class TcpServer
{
  private TcpListener _tcpListener;

  private void StartServer()
  {
    int port = 13008;
    var hostAddress = IPAddress.Parse("127.0.0.1");
    _tcpListener = new TcpListener(hostAddress, port);
    _tcpListener.Start();

    byte[] buffer = new byte[256];

    string ;

  }
}
