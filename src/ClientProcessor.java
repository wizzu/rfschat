import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class ClientProcessor implements Runnable {

  private char[] inputChars = new char[Client.INPUT_BUFFER_SIZE];

  private Selector clientInputSelector;
  private Selector clientOutputSelector;
  private AllClients clients;

  private Set<Client> registerQueue = new HashSet<Client>();
  private Set<Client> deRegisterQueue = new HashSet<Client>();

  public ClientProcessor(AllClients pClients) throws IOException {
    clients = pClients;

    try {
      clientInputSelector = Selector.open();
    } catch (IOException e) {
      // Note: Expect this error to never happen.
      System.out.println("Fatal error: Error opening client input selector");
      throw e;
    }

    try {
      clientOutputSelector = Selector.open();
    } catch (IOException e) {
      // Note: Expect this error to never happen.
      System.out.println("Fatal error: Error opening client output selector");
      throw e;
    }
  }

  /**
   *
   * @param chars
   * @return 0 if no newline, otherwise the number of characters in the first line, including CR + LF
   */
  private int lineSize(char[] chars) {
    int lineSize = 0;
    char c;
    for (int i = 0; i < chars.length; i++) {
      c = chars[i];
      if (c == '\n' || c == '\r') {
        lineSize = i;
        // check if we have CR + LF, is the next character LF?
        if (c == '\r' && i+1 < chars.length) {
          if (chars[i+1] == '\n') {
            lineSize = i + 1; // found LF after CR
          }
        }
        break;
      }
    }
    return lineSize;
  }

  private void handleCommand(String string, Client client) {
    if ("HELP".equals(string)) {
      client.sendMessage("Available commands:");
      client.sendMessage("HELP - this help text");
      client.sendMessage("NICK - change nickname");
      client.sendMessage("WHO - list of all connected users");
      client.sendMessage("QUIT - disconnect from service");
    } else if ("WHO".equals(string)) {
      client.sendMessage("Connected users:");
      for (Client c : clients.getClients()) {
        client.sendMessage(c.getUsername());
      }
    } else if (string.startsWith("NICK ")) {
      String newUsername = string.substring(5).trim();
      if (newUsername.length() > 0) {
        if (clients.findByUsername(newUsername) != null) {
          client.sendMessage("Sorry, nickname " + newUsername + " is already in use by someone else.");
        } else {
          String oldUsername = client.getUsername();
          client.setUsername(newUsername);
          clients.sendMessageToAll(oldUsername + " changed nickname to " + newUsername, null);
        }
      }
    } else if ("QUIT".equals(string)) {
      clients.removeClient(client);
      clients.sendMessageToAll(client.getUsername() + " has left the chat", null);
      removeClient(client);
      System.out.println("Disconnecting client " + client.toString());
    } else {
      // Simply send message to everyone else
      clients.sendMessageToAll(client.getUsername() + ": " + string, client);
    }
  }

  private void readFromSocket(SelectionKey key) throws IOException {
    Client client = (Client) key.attachment();

    StringBuilder str = new StringBuilder();
    ByteBuffer inputBuffer = client.getInputBuffer();

    while (true) {
      SocketChannel socketChannel = (SocketChannel) key.channel();
      int bytesRead = socketChannel.read(inputBuffer);
      if (bytesRead < 1) {
        break;
      }
      inputBuffer.flip();

      CharBuffer charBuffer = Charset.defaultCharset().decode(inputBuffer);

      // Note: I'm sure this could be prettier...

      // We need to read the bytes/characters from the input buffer
      // (inputBuffer/charBuffer) and see if there's a complete line.
      // If there isn't, then keep the bytes in the buffer and wait
      // for more data to arrive.
      // That's why the temporary inputChars array is used together
      // with the lineSize() function.
      charBuffer.mark();
      Arrays.fill(inputChars, '\u0000'); // zero the array
      charBuffer.get(inputChars, 0, Math.min(charBuffer.remaining(), inputChars.length)); // read characters to character array
      charBuffer.reset();
      int lineCharCount = lineSize(inputChars);
      if (lineCharCount > 0) {
        // We have characters forming a line (string terminated by CR / LF).
        charBuffer.get(inputChars, 0, lineCharCount);
        str.append(inputChars, 0, lineCharCount);
      }
      charBuffer.compact();
      inputBuffer.compact();
    }

    if (str.length() > 0) {
      String command = str.toString().trim();
      System.out.println("Client " + client.toString() + ": " + command);
      handleCommand(command, client);
    }
  }

  private void writeToSocket(SelectionKey key, Client client) throws IOException {
    SocketChannel socketChannel = (SocketChannel) key.channel();
    ByteBuffer outputBuffer = client.getOutputBuffer();
    outputBuffer.flip();
    socketChannel.write(outputBuffer);
    outputBuffer.compact();
  }

  private void processClientInput() {
    try {
      // Use non-blocking select for input.
      clientInputSelector.selectNow();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    Set<SelectionKey> selectedKeys = clientInputSelector.selectedKeys();
    Iterator<SelectionKey> selectedKeyIterator = selectedKeys.iterator();

    while(selectedKeyIterator.hasNext()) {
      SelectionKey key = selectedKeyIterator.next();

      if (!key.isValid()) {
        continue;
      }

      // Should only ever be isReadable() == true, but check anyway just to make sure.
      if (key.isReadable()) {
        try {
          readFromSocket(key);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      // Done handling this SelectionKey.
      selectedKeyIterator.remove();
    }

    selectedKeys.clear(); // Should be empty at this point but ensure it is.
  }

  private void processClientOutput() {
    try {
      // Use non-blocking select for output.
      clientOutputSelector.selectNow();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    Set<SelectionKey> selectedKeys = clientOutputSelector.selectedKeys();
    Iterator<SelectionKey> selectedKeyIterator = selectedKeys.iterator();

    while(selectedKeyIterator.hasNext()) {
      SelectionKey key = selectedKeyIterator.next();

      if (!key.isValid()) {
        continue;
      }

      Client client = (Client) key.attachment();
      // Should only ever be isWritable() == true, but check anyway just to make sure.
      if (client.hasPendingOutput() && key.isWritable()) {
        try {
          writeToSocket(key, client);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      // Done handling this SelectionKey.
      selectedKeyIterator.remove();
    }

    selectedKeys.clear(); // Should be empty at this point but ensure it is.
  }

  private void registerPendingClients() {
    synchronized (registerQueue) {
      Iterator<Client> clientIter = registerQueue.iterator();
      while (clientIter.hasNext()) {
        Client client = clientIter.next();
        SelectionKey selectionKey;

        try {
          selectionKey = client.getClientSocketChannel().register(clientInputSelector, SelectionKey.OP_READ);
          selectionKey.attach(client);
          client.setInputSelectionKey(selectionKey);
        } catch (ClosedChannelException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          selectionKey = client.getClientSocketChannel().register(clientOutputSelector, SelectionKey.OP_WRITE);
          selectionKey.attach(client);
          client.setOutputSelectionKey(selectionKey);
        } catch (ClosedChannelException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        clientIter.remove();
      }
    }
  }

  private void deRegisterPendingClients() {
    synchronized (deRegisterQueue) {
      Iterator<Client> clientIter = deRegisterQueue.iterator();
      while (clientIter.hasNext()) {
        Client client = clientIter.next();
        client.getInputSelectionKey().cancel();
        client.getOutputSelectionKey().cancel();
        SocketChannel channel = client.getClientSocketChannel();
        if (channel.isConnected()) {
          try {
            channel.close();
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        clientIter.remove();
      }
    }
  }

  public void run() {
    while (true) {
      processClientInput();
      processClientOutput();
      registerPendingClients();
      deRegisterPendingClients();
      try {
        Thread.sleep(5);  // sleep 5 ms before next loop
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  public void addClient(Client newClient) {
    synchronized (registerQueue) {
      registerQueue.add(newClient);
    }
    clientInputSelector.wakeup();
    clientOutputSelector.wakeup();
  }

  public void removeClient(Client client) {
    synchronized (deRegisterQueue) {
      deRegisterQueue.add(client);
    }
    clientInputSelector.wakeup();
    clientOutputSelector.wakeup();
  }
}
