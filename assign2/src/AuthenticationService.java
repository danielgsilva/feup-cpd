
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for handling user authentication and registration. Uses a file to
 * persist user credentials and implements thread-safe operations.
 */
public class AuthenticationService {

    private static final String USERS_FILE = "users.txt";
    private final Map<String, String> users; 
    private final ReentrantReadWriteLock lock;

    /**
     * Create a new authentication service. Loads users from file if available.
     */
    public AuthenticationService() {
        this.users = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();

        loadUsers();
    }

    /**
     * Load users from the users file.
     */
    private void loadUsers() {
        Path path = Paths.get(USERS_FILE);

        if (!Files.exists(path)) {
            registerUser("admin", "admin");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    lock.writeLock().lock();
                    try {
                        users.put(parts[0], parts[1]);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            }
            //System.out.println("Loaded " + users.size() + " users.");
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    /**
     * Save users to the users file.
     */
    private void saveUsers() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(USERS_FILE))) {
            lock.readLock().lock();
            try {
                for (Map.Entry<String, String> entry : users.entrySet()) {
                    writer.write(entry.getKey() + ":" + entry.getValue());
                    writer.newLine();
                }
            } finally {
                lock.readLock().unlock();
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }

    /**
     * Register a new user.
     *
     * @param username The username
     * @param password The password
     * @return true if registration successful, false if username already exists
     */
    public boolean registerUser(String username, String password) {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }

        lock.writeLock().lock();
        try {
            if (users.containsKey(username)) {
                return false;
            }

            users.put(username, hashPassword(password));
            saveUsers();
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Authenticate a user.
     *
     * @param username The username
     * @param password The password
     * @return true if authentication successful, false otherwise
     */
    public boolean authenticateUser(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            String storedHash = users.get(username);
            if (storedHash == null) {
                return false;
            }

            return storedHash.equals(hashPassword(password));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Hash a password using SHA-256.
     *
     * @param password The password to hash
     * @return The hashed password
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Warning: Password hashing not available. Using plaintext.");
            return password;
        }
    }
}
