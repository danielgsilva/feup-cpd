
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a chat room in the system. Contains messages and connected users.
 */
public class Room {

    private final String name;
    private final List<Message> messages;
    private final List<ClientHandler> connectedUsers;
    private final ReentrantReadWriteLock messagesLock;
    private final ReentrantReadWriteLock usersLock;

    /**
     * Create a new chat room with the given name.
     *
     * @param name The name of the room
     */
    public Room(String name) {
        this.name = name;
        this.messages = new ArrayList<>();
        this.connectedUsers = new ArrayList<>();
        this.messagesLock = new ReentrantReadWriteLock();
        this.usersLock = new ReentrantReadWriteLock();
    }

    /**
     * Get the name of the room.
     *
     * @return The room name
     */
    public String getName() {
        return name;
    }

    /**
     * Add a message to the room.
     *
     * @param message The message to add
     */
    public void addMessage(Message message) {
        messagesLock.writeLock().lock();
        try {
            messages.add(message);
        } finally {
            messagesLock.writeLock().unlock();
        }

        broadcastMessage(message);
    }

    /**
     * Get all messages in the room.
     *
     * @return A list of messages
     */
    public List<Message> getMessages() {
        messagesLock.readLock().lock();
        try {
            return new ArrayList<>(messages);
        } finally {
            messagesLock.readLock().unlock();
        }
    }

    /**
     * Add a user to the room.
     *
     * @param handler The client handler for the user
     */
    public void addUser(ClientHandler handler) {
        usersLock.writeLock().lock();
        try {
            if (!connectedUsers.contains(handler)) {
                connectedUsers.add(handler);

                Message enterMessage = new Message(
                        "SYSTEM",
                        "[" + handler.getUsername() + " enters the room]"
                );
                addMessage(enterMessage);
            }
        } finally {
            usersLock.writeLock().unlock();
        }
    }

    /**
     * Remove a user from the room.
     *
     * @param handler The client handler for the user
     */
    public void removeUser(ClientHandler handler) {
        usersLock.writeLock().lock();
        try {
            if (connectedUsers.remove(handler)) {
                // Add system message
                Message leaveMessage = new Message(
                        "SYSTEM",
                        "[" + handler.getUsername() + " leaves the room]"
                );
                addMessage(leaveMessage);
            }
        } finally {
            usersLock.writeLock().unlock();
        }
    }

    /**
     * Get the number of users in the room.
     *
     * @return The number of connected users
     */
    public int getUserCount() {
        usersLock.readLock().lock();
        try {
            return connectedUsers.size();
        } finally {
            usersLock.readLock().unlock();
        }
    }

    /**
     * Broadcast a message to all users in the room.
     *
     * @param message The message to broadcast
     */
    private void broadcastMessage(Message message) {
        usersLock.readLock().lock();
        try {
            for (ClientHandler handler : connectedUsers) {
                handler.sendMessage(message, this.name);
            }
        } finally {
            usersLock.readLock().unlock();
        }
    }
}
