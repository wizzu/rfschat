import java.io.IOException;
import java.net.ServerSocket;


public class ChatServer {
  private int port;
  private ServerSocket listenSocket;

  public ChatServer(int pPort) {
    this.port = pPort;
  }

  public void runServer() throws IOException {
    try {
      listenSocket = new ServerSocket(port);
    } catch (IOException e) {
      System.out.println("Error opening socket on port " + port + ", failed to start server");
      throw e;
    }

    // TODO: add server shutdown control
    boolean running = true;
    while (running) {
      try {
        listenSocket.accept();
      } catch (IOException e) {
        e.printStackTrace();
        System.out.println("Error accepting new connection");
      }
    }

    try {
      listenSocket.close();
    } catch (IOException e) {
      System.out.println("Error closing socket on port " + port + " during stop server");
      throw e;
    }
  }

  private static void printUsage() {
    System.out.println("Usage: ChatServer [port]");
  }

  private static int startServer(int pPort) {
    int exitCode = 0;
    ChatServer server = new ChatServer(pPort);
    try {
      server.runServer();
    } catch (IOException e) {
      e.printStackTrace();
      exitCode = 2;
    }
    return exitCode;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      printUsage();
      System.exit(1);
    }

    int port = Integer.parseInt(args[0]);

    System.exit(startServer(port));
  }

}
