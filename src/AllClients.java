import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class AllClients {

  private Set<Client> clients;
  private Random random;

  public AllClients() {
    clients = new HashSet<Client>();
    random = new Random();
  }

  public void addClient(Client client) {
    clients.add(client);
  }

  public void removeClient(Client client) {
    clients.remove(client);
  }

  public String generateRandomUniqueUsername() {
    String username = null;

    boolean generateNewName = true;
    while (generateNewName) {
      int randomNum = random.nextInt(9000) + 1000; // range: 1000 .. 9999
      username = "User" + randomNum;

      generateNewName = false; // start assuming new name is unique
      for (Client client : clients) {
        if (username.equalsIgnoreCase(client.getUsername())) {
          // new name was not unique, try again
          generateNewName = true;
          break;
        }
      }
    }

    return username;
  }

}
