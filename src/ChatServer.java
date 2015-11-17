import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class ChatServer {
  private int port;
  private AllClients clients;
  private ClientProcessor clientInputProcessor;
  private Thread clientInputThread;

  public ChatServer(int pPort) {
    this.port = pPort;
    clients = new AllClients();
  }

  private void handleNewConnection(SocketChannel pSocketChannel) throws IOException {
    pSocketChannel.configureBlocking(false);
    Client newClient = new Client(clients.getNextClientId(), pSocketChannel);
    newClient.setUsername(clients.generateRandomUniqueUsername());
    clients.addClient(newClient);
    clientInputProcessor.addClient(newClient);
    newClient.sendMessage("Welcome, your nickname is " + newClient.getUsername());
    newClient.sendMessage("Use command 'HELP' to get list of commands");
    clients.sendMessageToAll(newClient.getUsername() + " has joined the chat", newClient);
    System.out.println("New client connected from " + pSocketChannel.getRemoteAddress().toString() + ", id " + newClient.getId() + ", assigned username " + newClient.getUsername());
  }

  public void runServer() throws IOException {
    // Start client input processing thread.
    clientInputProcessor = new ClientProcessor(clients);
    clientInputThread = new Thread(clientInputProcessor, "RFSChatClientInputThread");
    clientInputThread.setDaemon(true);
    clientInputThread.start();
    System.out.println("Client input processing thread started");

    // Open the socket to listen on.
    ServerSocketChannel serverSocketChannel = null;
    try {
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.socket().bind(new InetSocketAddress(port));
    } catch (IOException e) {
      System.out.println("Fatal error: Error opening socket on port " + port + ", failed to start server");
      throw e;
    }
    System.out.println("Started listening for connection on local port " + port);


    // TODO: Add server shutdown control.
    // We'd need to convert the accept handling to have asynchronous accept()
    // with timeouts instead of blocking accept(), in order to be able to check
    // for shutdown conditions between incoming connection accepts.

    // Server main loop, waiting for incoming clients.
    System.out.println("Waiting for clients to connect...");
    boolean running = true;
    while (running) {
      SocketChannel socketChannel = null;
      try {
        socketChannel = serverSocketChannel.accept();
        handleNewConnection(socketChannel);
      } catch (IOException e) {
        e.printStackTrace();
        System.out.println("Error accepting new connection");
        if (socketChannel != null) {
          socketChannel.close();
        }
      }
    }


    // Currently there is no way to tell the server to shutdown,
    // but if there was, the following code attempts to shutdown
    // cleanly.
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      System.out.println("Error closing socket on port " + port + " during stop server");
      throw e;
    }

    clients.sendMessageToAll("Server is terminating", null);
    try {
      Thread.sleep(500); // wait 500 ms so the messages get delivered
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (Client client : clients.getClients()) {
      try {
        client.getClientSocketChannel().close();
      } catch (IOException e) {
        e.printStackTrace();
      }
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
