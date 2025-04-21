package src.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class RoomManager {
    private static final Map<String, Room> rooms = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public static Room getRoom(String name) {
        lock.lock();
        try {
            return rooms.computeIfAbsent(name, Room::new);
        } finally {
            lock.unlock();
        }
    }
}