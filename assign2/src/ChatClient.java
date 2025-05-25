
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Client for the chat application. Handles connection to the server and
 * provides methods for authentication and messaging. Supports automatic
 * reconnection with token-based authentication.
 */
public class ChatClient {

    private String host;
    private int port;
    private SSLSocket socket;
    private SSLSocketFactory sslSocketFactory;
    private PrintWriter out;
    private BufferedReader in;
    private MessageListener listener;
    private Thread listenerThread;
    private boolean connected;
    private boolean authenticated;
    private String username;
    private String currentRoom;
    private final ReentrantLock lock;

    private final List<ChatClientListener> listeners;

    private String authToken;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 3000;
    private boolean autoReconnect = true;

    /**
     * Create a new chat client.
     *
     * @param host The server host
     * @param port The server port
     */
    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.connected = false;
        this.authenticated = false;
        this.currentRoom = null;
        this.authToken = null;
        this.lock = new ReentrantLock();
        this.listeners = new ArrayList<>();
    }

    /**
     * Connect to the server.
     *
     * @return true if connection successful, false otherwise
     */
    public boolean connect() {
        try {
            System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");

            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start message listener
            listener = new MessageListener(this, in);
            listenerThread = Thread.startVirtualThread(listener);

            connected = true;

            if (authToken != null) {
                attemptReconnection();
            }

            return true;
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        autoReconnect = false;

        if (connected) {
            try {
                // Stop the listener
                if (listener != null) {
                    listener.stop();
                }

                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error disconnecting: " + e.getMessage());
            } finally {
                connected = false;
            }
        }
    }

    /**
     * Handle connection loss and attempt reconnection.
     */
    void handleConnectionLoss() {
        if (!autoReconnect) {
            return;
        }

        System.out.println("Server connection lost. Attempting to reconnect...");
        connected = false;

        notifyListeners(ClientEvent.CONNECTION_LOST, null);

        Thread.startVirtualThread(this::performReconnection);
    }

    /**
     * Perform reconnection attempts with exponential backoff.
     */
    private void performReconnection() {
        int attempts = 0;
        int delay = RECONNECT_DELAY_MS;

        while (attempts < MAX_RECONNECT_ATTEMPTS && autoReconnect && !connected) {
            attempts++;

            System.out.println("Reconnection attempt " + attempts + "/" + MAX_RECONNECT_ATTEMPTS);

            try {
                Thread.sleep(delay);

                if (reconnectToServer()) {
                    //System.out.println("Reconnection successful!");
                    notifyListeners(ClientEvent.CONNECTION_RESTORED, null);
                    return;
                }

                delay = Math.min(delay * 2, 30000);
                delay += (int) (Math.random() * 1000);

            } catch (InterruptedException e) {
                System.err.println("Reconnection interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.err.println("Failed to reconnect after " + MAX_RECONNECT_ATTEMPTS + " attempts");
        notifyListeners(ClientEvent.RECONNECTION_FAILED, null);
    }

    /**
     * Attempt to reconnect to the server and restore session.
     *
     * @return true if reconnection successful, false otherwise
     */
    private boolean reconnectToServer() {
        try {
            closeResources();

            System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");

            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start new message listener
            listener = new MessageListener(this, in);
            listenerThread = Thread.startVirtualThread(listener);

            connected = true;

            if (authToken != null) {
                return attemptReconnection();
            }

            return true;

        } catch (IOException e) {
            System.err.println("Error during reconnection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Attempt to restore session using authentication token.
     *
     * @return true if session restored successfully, false otherwise
     */
    private boolean attemptReconnection() {
        if (authToken == null) {
            return false;
        }

        out.println("RECONNECT " + authToken);

        long startTime = System.currentTimeMillis();
        long timeout = 5000;

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                Thread.sleep(100);
                lock.lock();
                try {
                    if (authenticated) {
                        return true;
                    }
                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.err.println("Failed to restore session");
        authToken = null;
        return false;
    }

    /**
     * Close existing connection resources.
     */
    private void closeResources() {
        try {
            if (listener != null) {
                listener.stop();
            }

            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }

            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            System.err.println("Error closing resources during reconnection: " + e.getMessage());
        }
    }

    /**
     * Register a new user.
     *
     * @param username The username
     * @param password The password
     * @return true if registration successful, false otherwise
     */
    public boolean register(String username, String password) {
        if (!connected) {
            return false;
        }
        out.println("REGISTER " + username + " " + password);

        return true;
    }

    /**
     * Login with the given credentials.
     *
     * @param username The username
     * @param password The password
     * @return true if command sent successfully, false otherwise
     */
    public boolean login(String username, String password) {
        if (!connected) {
            return false;
        }

        this.username = username;
        out.println("LOGIN " + username + " " + password);

        return true;
    }

    /**
     * Logout from the server.
     */
    public void logout() {
        if (connected && authenticated) {
            out.println("LOGOUT");
        }
    }

    /**
     * Get a list of available rooms.
     */
    public void requestRoomList() {
        if (connected && authenticated) {
            out.println("LIST_ROOMS");
        }
    }

    /**
     * Create a new chat room.
     *
     * @param roomName The name of the room
     */
    public void createRoom(String roomName) {
        if (connected && authenticated) {
            out.println("CREATE_ROOM " + roomName);
        }
    }

    /**
     * Create a new AI chat room.
     *
     * @param roomName The name of the room
     * @param prompt The initial prompt/instructions for the AI
     */
    public void createAiRoom(String roomName, String prompt) {
        if (connected && authenticated) {
            out.println("CREATE_AI_ROOM " + roomName + " " + prompt);
        }
    }

    /**
     * Join a chat room.
     *
     * @param roomName The name of the room
     */
    public void joinRoom(String roomName) {
        if (connected && authenticated) {
            out.println("JOIN_ROOM " + roomName);
        }
    }

    /**
     * Leave the current chat room.
     */
    public void leaveRoom() {
        if (connected && authenticated && currentRoom != null) {
            out.println("LEAVE_ROOM");
        }
    }

    /**
     * Send a message to the current room.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean sendMessage(String message) {
        if (connected && authenticated && currentRoom != null) {
            out.println("MESSAGE " + currentRoom + " " + message);
            return true;
        }
        return false;
    }

    /**
     * Handle a message from the server.
     *
     * @param message The message
     */
    void handleServerMessage(String message) {
        System.out.println("Received message from server: " + message);

        String[] parts = message.split(" ");
        String command = parts[0];

        switch (command) {
            case "REGISTER_SUCCESS":
                notifyListeners(ClientEvent.REGISTER_SUCCESS, null);
                break;

            case "REGISTER_FAILURE":
                notifyListeners(ClientEvent.REGISTER_FAILURE, null);
                break;

            case "LOGIN_SUCCESS":
                lock.lock();
                try {
                    authenticated = true;
                    if (parts.length > 1) {
                        authToken = parts[1];
                    }
                } finally {
                    lock.unlock();
                }
                notifyListeners(ClientEvent.LOGIN_SUCCESS, null);

                break;

            case "LOGIN_FAILURE":
                notifyListeners(ClientEvent.LOGIN_FAILURE, null);
                break;

            case "RECONNECT_SUCCESS":
                lock.lock();
                try {
                    authenticated = true;
                } finally {
                    lock.unlock();
                }
                notifyListeners(ClientEvent.RECONNECTION_SUCCESS, null);
                break;

            case "RECONNECT_FAILURE":
                lock.lock();
                try {
                    authenticated = false;
                    authToken = null;
                } finally {
                    lock.unlock();
                }
                notifyListeners(ClientEvent.RECONNECTION_FAILED, null);
                break;

            case "LOGOUT_SUCCESS":
                lock.lock();
                try {
                    authenticated = false;
                    currentRoom = null;
                    authToken = null;
                } finally {
                    lock.unlock();
                }
                notifyListeners(ClientEvent.LOGOUT_SUCCESS, null);
                break;

            case "ROOMS":
                if (parts.length > 1) {
                    String[] roomNames = Arrays.copyOfRange(parts, 1, parts.length);
                    notifyListeners(ClientEvent.ROOM_LIST, roomNames);
                }
                break;

            case "ROOM_CREATED":
                if (parts.length > 1) {
                    notifyListeners(ClientEvent.ROOM_CREATED, parts[1]);
                }
                break;

            case "ROOM_EXISTS":
                if (parts.length > 1) {
                    notifyListeners(ClientEvent.ROOM_EXISTS, parts[1]);
                }
                break;

            case "JOINED":
                if (parts.length > 1) {
                    lock.lock();
                    try {
                        currentRoom = parts[1];
                    } finally {
                        lock.unlock();
                    }
                    notifyListeners(ClientEvent.ROOM_JOINED, parts[1]);
                }
                break;

            case "LEFT_ROOM":
                if (parts.length > 1) {
                    lock.lock();
                    try {
                        currentRoom = null;
                    } finally {
                        lock.unlock();
                    }
                    notifyListeners(ClientEvent.ROOM_LEFT, parts[1]);
                }
                break;

            case "MESSAGE":
                if (parts.length > 3) {
                    String[] messageParts = message.split(" ", 4);
                    String roomName = messageParts[1];
                    String messageSender = messageParts[2];
                    String messageContent = messageParts[3];
                    if (!messageSender.equals("SYSTEM")) {
                        messageContent = messageSender + ": " + messageContent;
                    }
                    notifyListeners(ClientEvent.MESSAGE_RECEIVED, new String[]{roomName, messageContent});
                }
                break;

            default:
                break;
        }
    }

    /**
     * Check if the client is connected to the server.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Check if the client is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        lock.lock();
        try {
            return authenticated;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the current username.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the current room.
     *
     * @return The current room name, or null if not in a room
     */
    public String getCurrentRoom() {
        lock.lock();
        try {
            return currentRoom;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add a client event listener.
     *
     * @param listener The listener to add
     */
    public void addListener(ChatClientListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a client event listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(ChatClientListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners of an event.
     *
     * @param event The event
     * @param data Event data
     */
    private void notifyListeners(ClientEvent event, Object data) {
        for (ChatClientListener ccl : listeners) {
            ccl.onEvent(event, data);
        }
    }
}
