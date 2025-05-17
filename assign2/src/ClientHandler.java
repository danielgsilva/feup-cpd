
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles a client connection. Manages authentication, room operations, and
 * message processing.
 */
public class ClientHandler {

    private final Socket socket;
    private final AuthenticationService authService;
    private final RoomManager roomManager;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private String currentRoom;
    private boolean authenticated;
    private boolean running;

    /**
     * Create a new client handler.
     *
     * @param socket The client socket
     * @param authService The authentication service
     * @param roomManager The room manager
     */
    public ClientHandler(Socket socket, AuthenticationService authService, RoomManager roomManager) {
        this.socket = socket;
        this.authService = authService;
        this.roomManager = roomManager;
        this.authenticated = false;
        this.currentRoom = null;
        this.running = false;
    }

    /**
     * Get the username of the connected client.
     *
     * @return The username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Send a message to the client.
     *
     * @param message The message to send
     * @param roomName The room the message is from
     */
    public void sendMessage(Message message, String roomName) {
        if (this.out != null) {
            this.out.println("MESSAGE " + roomName + " " + message.toProtocolString());
        }
    }

    /**
     * Handle the client connection.
     *
     * @throws IOException If an I/O error occurs
     */
    public void handle() throws IOException {
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);

            this.running = true;

            // Handle client messages
            while (this.running) {
                String input = in.readLine();
                if (input == null) {
                    break;
                }

                processInput(input);
            }
        } finally {
            cleanup();
        }
    }

    /**
     * Process client input.
     *
     * @param input The client input
     */
    private void processInput(String input) {
        System.out.println("Received message from client (port " + socket.getPort() + "): " + input);

        String[] parts = input.split(" ", 3);
        String command = parts[0];

        if (!this.authenticated) {
            handleUnauthenticatedCommands(command, parts);
        } else {
            handleAuthenticatedCommands(command, parts);
        }
    }

    /**
     * Handle commands from unauthenticated clients.
     *
     * @param command The command
     * @param parts The command parts
     */
    private void handleUnauthenticatedCommands(String command, String[] parts) {
        switch (command) {
            case "REGISTER":
                if (parts.length >= 3) {
                    String user = parts[1];
                    String pass = parts[2];
                    if (authService.registerUser(user, pass)) {
                        out.println("REGISTER_SUCCESS");
                    } else {
                        out.println("REGISTER_FAILURE");
                    }
                } else {
                    out.println("INVALID_COMMAND");
                }
                break;

            case "LOGIN":
                if (parts.length >= 3) {
                    String user = parts[1];
                    String pass = parts[2];
                    if (authService.authenticateUser(user, pass)) {
                        this.username = user;
                        this.authenticated = true;
                        out.println("LOGIN_SUCCESS");
                    } else {
                        out.println("LOGIN_FAILURE");
                    }
                } else {
                    out.println("INVALID_COMMAND");
                }
                break;

            default:
                out.println("UNAUTHENTICATED");
                break;
        }
    }

    /**
     * Handle commands from authenticated clients.
     *
     * @param command The command
     * @param parts The command parts
     */
    private void handleAuthenticatedCommands(String command, String[] parts) {
        switch (command) {
            case "LIST_ROOMS":
                out.println("ROOMS " + String.join(" ", roomManager.getRoomNames()));
                break;

            case "CREATE_ROOM":
                if (parts.length >= 2) {
                    String roomName = parts[1];
                    if (roomManager.createRoom(roomName)) {
                        out.println("ROOM_CREATED " + roomName);
                    } else {
                        out.println("ROOM_EXISTS " + roomName);
                    }
                } else {
                    out.println("INVALID_COMMAND");
                }
                break;

            case "JOIN_ROOM":
                if (parts.length >= 2) {
                    String roomName = parts[1];

                    // Leave current room if any
                    if (this.currentRoom != null) {
                        roomManager.removeUserFromRoom(this.currentRoom, this);
                    }

                    // Join new room
                    if (roomManager.addUserToRoom(roomName, this)) {
                        this.currentRoom = roomName;
                        out.println("JOINED " + roomName);
                    } else {
                        out.println("ROOM_NOT_FOUND " + roomName);
                    }
                } else {
                    out.println("INVALID_COMMAND");
                }
                break;

            case "LEAVE_ROOM":
                if (this.currentRoom != null) {
                    roomManager.removeUserFromRoom(this.currentRoom, this);
                    out.println("LEFT_ROOM " + this.currentRoom);
                    this.currentRoom = null;
                } else {
                    out.println("NOT_IN_ROOM");
                }
                break;

            case "MESSAGE":
                if (parts.length >= 3 && this.currentRoom != null) {
                    String roomName = parts[1];
                    String messageContent = parts[2];

                    if (roomName.equals(this.currentRoom)) {
                        Message message = new Message(this.username, messageContent);
                        roomManager.addMessageToRoom(roomName, message);
                    } else {
                        out.println("NOT_IN_ROOM " + roomName);
                    }
                } else {
                    out.println("INVALID_COMMAND");
                }
                break;

            case "LOGOUT":
                if (this.currentRoom != null) {
                    roomManager.removeUserFromRoom(this.currentRoom, this);
                }
                this.authenticated = false;
                this.username = null;
                this.currentRoom = null;
                out.println("LOGOUT_SUCCESS");
                break;

            default:
                out.println("UNKNOWN_COMMAND");
                break;
        }
    }

    /**
     * Clean up resources when the connection is closed.
     */
    private void cleanup() {
        this.running = false;

        // Leave current room if any
        if (this.authenticated && this.currentRoom != null) {
            roomManager.removeUserFromRoom(this.currentRoom, this);
        }

        // Close streams and socket
        try {
            if (this.in != null) {
                in.close();
            }
            if (this.out != null) {
                out.close();
            }
            if (this.socket != null && !this.socket.isClosed()) {
                this.socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}
