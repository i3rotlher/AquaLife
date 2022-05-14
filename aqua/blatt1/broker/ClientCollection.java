package aqua.blatt1.broker;

import java.util.ArrayList;
import java.util.List;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	private class Client {
		final String id;
		final T client;
		private long timestamp;

		Client(String id, T client, long timestamp) {
			this.id = id;
			this.client = client;
			this.timestamp = timestamp;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client, long timestamp) {
		clients.add(new Client(id, client, timestamp));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		clients.remove(index);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClient(int index) {
		return clients.get(index).client;
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighorOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighorOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

	public void updateTimestamp(int index, long timestamp) {
		clients.get(index).timestamp = timestamp;
	}

	public long getTimestamp(String id) {
		return clients.get(indexOf(id)).timestamp;
	}

	public List<String> getExpiredClientsIDs(int leaseDuration) {
		long now = System.currentTimeMillis();
		List<String> expiredClients = new ArrayList<>();

		for (Client c : clients) {
			if (now - c.timestamp > leaseDuration) {
				expiredClients.add(c.id);
			}
		}

		return expiredClients;
	}

}
