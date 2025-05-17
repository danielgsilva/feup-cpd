
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Console-based user interface for the chat client.
 */
public class ClientConsoleUI implements ChatClientListener {

    private final ChatClient client;
    private final Scanner scanner;
    private boolean running;

    // Latches for synchronization
    // These are used to wait for server responses
    private CountDownLatch authLatch;
    private CountDownLatch roomListLatch;
    private CountDownLatch joinRoomLatch;

    /**
     * Create a new console UI.
     *
     * @param host The server host
     * @param port The server port
     */
    public ClientConsoleUI(String host, int port) {
        this.client = new ChatClient(host, port);
        this.scanner = new Scanner(System.in);
        this.client.addListener(this);
        this.running = false;
    }

    /**
     * Start the UI.
     */
    public void start() {
        running = true;

        // Connect to server
        if (!client.connect()) {
            System.out.println("Failed to connect to server. Exiting.");
            return;
        }
        System.out.println("Connected to server.");

        // Main input loop
        while (running) {
            if (!client.isAuthenticated()) {
                showAuthMenu();
            } else if (client.getCurrentRoom() == null) {
                showRoomMenu();
            } else {
                showChatInterface();
            }
        }

        // Disconnect
        client.disconnect();
        System.out.println("Disconnected from server.");
    }

    /**
     * Show the authentication menu.
     */
    private void showAuthMenu() {
        System.out.println("\nAuthentication Menu");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                login();
                break;

            case "2":
                register();
                break;

            case "3":
                running = false;
                break;

            default:
                System.out.println("Invalid option.");
                break;
        }
    }

    /**
     * Show the room menu.
     */
    private void showRoomMenu() {
        System.out.println("\nRoom Menu");
        System.out.println("1. List rooms");
        System.out.println("2. Create room");
        System.out.println("3. Join room");
        System.out.println("4. Logout");
        System.out.println("5. Exit");
        System.out.println("Choose an option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                roomListLatch = new CountDownLatch(1);

                client.requestRoomList();

                try {
                    roomListLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while waiting for room list.");
                }
                break;

            case "2":
                createRoom();
                break;

            case "3":
                joinRoomLatch = new CountDownLatch(1);

                joinRoom();

                try {
                    joinRoomLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while waiting for join room.");
                }
                break;

            case "4":
                authLatch = new CountDownLatch(1);
                client.logout();
                try {
                    // Wait up to 3 seconds for registration response
                    if (!authLatch.await(3, TimeUnit.SECONDS)) {
                        System.out.println("Server response timeout. Please try again.");
                    }
                } catch (InterruptedException e) {
                    System.err.println("Registration interrupted.");
                }
                break;

            case "5":
                running = false;
                break;

            default:
                System.out.println("Invalid option.");
                break;
        }
    }

    /**
     * Show the chat interface.
     */
    private void showChatInterface() {
        System.out.println("\nRoom: " + client.getCurrentRoom());
        System.out.println("Enter message (or /leave to leave room, /quit to exit): ");
        String input = scanner.nextLine();

        if (input.equals("/leave")) {
            client.leaveRoom();
        } else if (input.equals("/quit")) {
            running = false;
        } else {
            client.sendMessage(input);
        }
    }

    /**
     * Login to the server.
     */
    private void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        // Create a latch to wait for response
        authLatch = new CountDownLatch(1);

        client.login(username, password);

        try {
            // Wait up to 5 seconds for authentication response
            if (!authLatch.await(5, TimeUnit.SECONDS)) {
                System.out.println("Server response timeout. Please try again.");
            }
        } catch (InterruptedException e) {
            System.err.println("Authentication interrupted.");
        }
    }

    /**
     * Register a new user.
     */
    private void register() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        // Create a latch to wait for response
        authLatch = new CountDownLatch(1);

        client.register(username, password);

        try {
            // Wait up to 5 seconds for registration response
            if (!authLatch.await(5, TimeUnit.SECONDS)) {
                System.out.println("Server response timeout. Please try again.");
            }
        } catch (InterruptedException e) {
            System.err.println("Registration interrupted.");
        }
    }

    /**
     * Create a new chat room.
     */
    private void createRoom() {
        System.out.print("Room name: ");
        String roomName = scanner.nextLine();

        client.createRoom(roomName);
    }

    /**
     * Join a chat room.
     */
    private void joinRoom() {
        System.out.print("Room name: ");
        String roomName = scanner.nextLine();

        client.joinRoom(roomName);
    }

    @Override
    public void onEvent(ClientEvent event, Object data) {
        switch (event) {
            case REGISTER_SUCCESS:
                System.out.println("Registration successful.");
                if (authLatch != null) {
                    authLatch.countDown();
                }
                break;

            case REGISTER_FAILURE:
                System.out.println("Registration failed.");
                if (authLatch != null) {
                    authLatch.countDown();
                }
                break;

            case LOGIN_SUCCESS:
                System.out.println("Login successful.");
                if (authLatch != null) {
                    authLatch.countDown();
                }
                break;

            case LOGIN_FAILURE:
                System.out.println("Login failed.");
                if (authLatch != null) {
                    authLatch.countDown();
                }
                break;

            case LOGOUT_SUCCESS:
                System.out.println("Logged out successfully.");
                if (authLatch != null) {
                    authLatch.countDown();
                }
                break;

            case ROOM_LIST:
                if (data instanceof String[] rooms) {
                    System.out.println("\nAvailable rooms:");
                    if (rooms.length == 0) {
                        System.out.println("No rooms available.");
                    } else {
                        for (String room : rooms) {
                            System.out.println("- " + room);
                        }
                    }
                }
                roomListLatch.countDown();
                break;

            case ROOM_CREATED:
                System.out.println("Room created: " + data);
                break;

            case ROOM_EXISTS:
                System.out.println("Room already exists: " + data);
                break;

            case ROOM_JOINED:
                System.out.println("Joined room: " + data);
                joinRoomLatch.countDown();
                break;

            case ROOM_LEFT:
                System.out.println("Left room: " + data);
                break;

            case MESSAGE_RECEIVED:
                if (data instanceof String[] messageData) {
                    String roomName = messageData[0];
                    String messageContent = messageData[1];

                    if (roomName.equals(client.getCurrentRoom())) {
                        System.out.println(messageContent);
                    }
                }
                break;
        }
    }

    /**
     * Main entry point for the client application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // java ClientConsoleUI [<host>] [<port>]

        String host = "localhost";
        int port = 1234;

        if (args.length > 0) {
            host = args[0];
        }

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port 1234.");
            }
        }

        ClientConsoleUI ui = new ClientConsoleUI(host, port);
        ui.start();
    }
}
