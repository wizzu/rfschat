import java.io.IOException;
import java.nio.channels.SocketChannel;


public class Client {
  private SocketChannel clientSocketChannel;
  private String username;

  public Client(SocketChannel pSocketChannel) throws IOException {
    clientSocketChannel = pSocketChannel;
  }

  public SocketChannel getClientSocketChannel() {
    return clientSocketChannel;
  }

  public void setClientSocketChannel(SocketChannel clientSocketChannel) {
    this.clientSocketChannel = clientSocketChannel;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

}
