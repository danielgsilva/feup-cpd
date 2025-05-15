package src.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class Room {
    private final String name;
    private final Set<ClientHandler> clients = new HashSet<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Room(String name) {
        this.name = name;
    }

    public void join(ClientHandler client) {
        lock.lock();
        try {
            // Remove o antigo client com mesmo username se existir
            clients.removeIf(c -> c.getUsername().equals(client.getUsername()));
            clients.add(client);
            broadcast("[Server] " + client.getUsername() + " has entered the room.");
        } finally {
            lock.unlock();
        }
    }

    public void leave(ClientHandler client) {
        lock.lock();
        try {
            clients.remove(client);
            broadcast("[Server] " + client.getUsername() + " has left the room.");
        } finally {
            lock.unlock();
        }
    }

    public void broadcast(String message) {
        lock.lock();
        try {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getMemberUsernames() {
        lock.lock();
        try {
            Set<String> usernames = new HashSet<>();
            for (ClientHandler client : clients) {
                usernames.add(client.getUsername());
            }
            return usernames;
        } finally {
            lock.unlock();
        }
    }

    public String getName() {
        return name;
    }
}
