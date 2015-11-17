import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class Client {
  public final static int INPUT_BUFFER_SIZE = 1024 * 4;
  public final static int OUTPUT_BUFFER_SIZE = 1024 * 4;

  // Connection details
  private SocketChannel clientSocketChannel;
  private ByteBuffer inputBuffer;
  private ByteBuffer outputBuffer;
  private SelectionKey inputSelectionKey;
  private SelectionKey outputSelectionKey;

  // User details
  private int id;
  private String username;
  private long createdOn;


  public Client(int pId, SocketChannel pSocketChannel) {
    this.createdOn = System.currentTimeMillis();
    this.inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
    this.outputBuffer = ByteBuffer.allocate(OUTPUT_BUFFER_SIZE);
    this.id = pId;
    this.clientSocketChannel = pSocketChannel;
  }

  public SocketChannel getClientSocketChannel() {
    return clientSocketChannel;
  }

  public void setClientSocketChannel(SocketChannel clientSocketChannel) {
    this.clientSocketChannel = clientSocketChannel;
  }

  public ByteBuffer getInputBuffer() {
    return inputBuffer;
  }

  public ByteBuffer getOutputBuffer() {
    return outputBuffer;
  }

  public boolean hasPendingOutput() {
    // This assumes the buffer is in "write" mode.
    return outputBuffer.position() > 0;
  }

  public SelectionKey getInputSelectionKey() {
    return inputSelectionKey;
  }

  public void setInputSelectionKey(SelectionKey selectionKey) {
    this.inputSelectionKey = selectionKey;
  }

  public SelectionKey getOutputSelectionKey() {
    return outputSelectionKey;
  }

  public void setOutputSelectionKey(SelectionKey selectionKey) {
    this.outputSelectionKey = selectionKey;
  }

  public int getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void sendMessage(String message) {
    StringBuilder str = new StringBuilder(message);
    str.append("\r\n");
    outputBuffer.put(str.toString().getBytes());
  }

  public String toString() {
    return "[" + getId() + "] " + getUsername();
  }
}
