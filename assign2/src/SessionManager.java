import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages user sessions, allowing reconnection to existing sessions.
 */
public class SessionManager {
    
    private final Map<String, UserSession> userSessions; 
    private final ReentrantReadWriteLock lock;
    
    /**
     * Create a new session manager.
     */
    public SessionManager() {
        this.userSessions = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    /**
     * Create or update a user session.
     * 
     * @param username The username
     * @param currentRoom The current room (can be null)
     * @param handler The client handler
     */
    public void createOrUpdateSession(String username, String currentRoom, ClientHandler handler) {
        lock.writeLock().lock();
        try {
            UserSession session = userSessions.get(username);
            if (session == null) {
                session = new UserSession(username);
                userSessions.put(username, session);
            }
            
            session.setCurrentRoom(currentRoom);
            session.setClientHandler(handler);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get a user session.
     * 
     * @param username The username
     * @return The user session, or null if not found
     */
    public UserSession getSession(String username) {
        lock.readLock().lock();
        try {
            return userSessions.get(username);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Remove a user session.
     * 
     * @param username The username
     */
    public void removeSession(String username) {
        lock.writeLock().lock();
        try {
            userSessions.remove(username);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Check if a user has an active session.
     * 
     * @param username The username
     * @return true if the user has an active session
     */
    public boolean hasActiveSession(String username) {
        lock.readLock().lock();
        try {
            return userSessions.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Represents a user session.
     */
    public static class UserSession {
        private final String username;
        private String currentRoom;
        private ClientHandler clientHandler;
        private final ReentrantReadWriteLock sessionLock;
        
        public UserSession(String username) {
            this.username = username;
            this.currentRoom = null;
            this.clientHandler = null;
            this.sessionLock = new ReentrantReadWriteLock();
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getCurrentRoom() {
            sessionLock.readLock().lock();
            try {
                return currentRoom;
            } finally {
                sessionLock.readLock().unlock();
            }
        }
        
        public void setCurrentRoom(String currentRoom) {
            sessionLock.writeLock().lock();
            try {
                this.currentRoom = currentRoom;
            } finally {
                sessionLock.writeLock().unlock();
            }
        }
        
        public ClientHandler getClientHandler() {
            sessionLock.readLock().lock();
            try {
                return clientHandler;
            } finally {
                sessionLock.readLock().unlock();
            }
        }
        
        public void setClientHandler(ClientHandler clientHandler) {
            sessionLock.writeLock().lock();
            try {
                this.clientHandler = clientHandler;
            } finally {
                sessionLock.writeLock().unlock();
            }
        }
    }
}