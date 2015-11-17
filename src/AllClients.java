import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class AllClients {

  private Set<Client> clients;
  private Random random;
  private int nextClientId = 1;

  public AllClients() {
    clients = new HashSet<Client>();
    random = new Random();
  }

  public void addClient(Client client) {
    synchronized (clients) {
      clients.add(client);
    }
  }

  public void removeClient(Client client) {
    synchronized (clients) {
      clients.remove(client);
    }
  }

  public Set<Client> getClients() {
    Set<Client> returnSet = new HashSet<Client>();
    synchronized (clients) {
      returnSet.addAll(clients);
    }
    return Collections.unmodifiableSet(returnSet);
  }

  public int getNextClientId() {
    int clientId = nextClientId;
    nextClientId++;
    return clientId;
  }

  public Client findByUsername(String username) {
    Client foundClient = null;
    for (Client client : clients) {
      if (username.equalsIgnoreCase(client.getUsername())) {
        foundClient = client;
        break;
      }
    }
    return foundClient;
  }

  public void sendMessageToAll(String message, Client excludeClient) {
    for (Client c : clients) {
      if (c != excludeClient) {
        c.sendMessage(message);
      }
    }
  }

  public String generateRandomUniqueUsername() {
    String username = null;

    boolean generateNewName = true;
    while (generateNewName) {
      int randomNum = random.nextInt(9000) + 1000; // range: 1000 .. 9999
      username = "User" + randomNum;

      generateNewName = (findByUsername(username) != null);
    }

    return username;
  }

}
