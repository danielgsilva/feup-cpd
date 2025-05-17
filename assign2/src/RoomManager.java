
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages chat rooms in the system. Handles room creation, deletion, and access
 * in a thread-safe manner.
 */
public class RoomManager {

    private final Map<String, Room> rooms;
    private final ReentrantReadWriteLock lock;

    /**
     * Create a new room manager.
     */
    public RoomManager() {
        this.rooms = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Create a new room with the given name.
     *
     * @param name The name of the room
     * @return true if the room was created, false if a room with that name
     * already exists
     */
    public boolean createRoom(String name) {
        lock.writeLock().lock();
        try {
            if (rooms.containsKey(name)) {
                return false;
            }

            rooms.put(name, new Room(name));
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get a room by name.
     *
     * @param name The name of the room
     * @return The room, or null if no such room exists
     */
    public Room getRoom(String name) {
        lock.readLock().lock();
        try {
            return rooms.get(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get a list of all room names.
     *
     * @return A list of room names
     */
    public List<String> getRoomNames() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rooms.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove a room.
     *
     * @param name The name of the room to remove
     * @return true if the room was removed, false if no such room exists
     */
    public boolean removeRoom(String name) {
        lock.writeLock().lock();
        try {
            Room room = rooms.get(name);
            if (room != null && room.getUserCount() == 0) {
                rooms.remove(name);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add a user to a room.
     *
     * @param roomName The name of the room
     * @param handler The client handler for the user
     * @return true if the user was added, false if the room doesn't exist
     */
    public boolean addUserToRoom(String roomName, ClientHandler handler) {
        Room room = getRoom(roomName);
        if (room != null) {
            room.addUser(handler);
            return true;
        }
        return false;
    }

    /**
     * Remove a user from a room.
     *
     * @param roomName The name of the room
     * @param handler The client handler for the user
     * @return true if the user was removed, false if the room doesn't exist
     */
    public boolean removeUserFromRoom(String roomName, ClientHandler handler) {
        Room room = getRoom(roomName);
        if (room != null) {
            room.removeUser(handler);
            return true;
        }
        return false;
    }

    /**
     * Add a message to a room.
     *
     * @param roomName The name of the room
     * @param message The message to add
     * @return true if the message was added, false if the room doesn't exist
     */
    public boolean addMessageToRoom(String roomName, Message message) {
        Room room = getRoom(roomName);
        if (room != null) {
            room.addMessage(message);
            return true;
        }
        return false;
    }
}
