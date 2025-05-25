
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
    private boolean headerShown = false;

    // Latches for synchronization
    // These are used to wait for server responses
    // Since our solution is asynchronous, we can get a better user experience
    // by waiting for server responses before proceeding with the UI flow
    private CountDownLatch authLatch;
    private CountDownLatch roomListLatch;
    private CountDownLatch joinRoomLatch;
    private CountDownLatch leaveRoomLatch;

    private static final String DEFAULT_AI_PROMPT
            = "You are a helpful assistant. Answer questions briefly and clearly.";

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

        if (!client.connect()) {
            System.out.println("Failed to connect to server. Exiting.");
            return;
        }
        System.out.println("Connected to server.");

        while (running) {
            if (!client.isAuthenticated()) {
                showAuthMenu();
                headerShown = false;
            } else if (client.getCurrentRoom() == null) {
                showRoomMenu();
                headerShown = false;
            } else {
                showChatInterface();
            }
        }

        client.disconnect();
        System.out.println("Disconnected from server.");
    }

    /**
     * Show the authentication menu.
     */
    private void showAuthMenu() {
        System.out.println("\n╔══════════════════════════╗");
        System.out.println("║     Authentication       ║");
        System.out.println("╠══════════════════════════╣");
        System.out.println("║ 1. Login                 ║");
        System.out.println("║ 2. Register              ║");
        System.out.println("║ 3. Exit                  ║");
        System.out.println("╚══════════════════════════╝");
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
                exitApplication();
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
        System.out.println("\n╔══════════════════════════╗");
        System.out.println("║         Room Menu        ║");
        System.out.println("╠══════════════════════════╣");
        System.out.println("║ 1. List rooms            ║");
        System.out.println("║ 2. Create room           ║");
        System.out.println("║ 3. Create AI room        ║");
        System.out.println("║ 4. Join room             ║");
        System.out.println("║ 5. Logout                ║");
        System.out.println("║ 6. Exit                  ║");
        System.out.println("╚══════════════════════════╝");
        System.out.println("Choose an option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                roomListLatch = new CountDownLatch(1);

                client.requestRoomList();

                try {
                    if (!running) {
                        return;
                    }
                    roomListLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while waiting for room list.");
                }
                break;

            case "2":
                createRoom();
                break;

            case "3":
                createAiRoom();
                break;

            case "4":
                joinRoomLatch = new CountDownLatch(1);

                joinRoom();

                try {
                    if (!running) {
                        return;
                    }
                    joinRoomLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while waiting for join room.");
                }
                break;

            case "5":
                authLatch = new CountDownLatch(1);
                client.logout();
                try {
                    if (!running) {
                        return;
                    }
                    if (!authLatch.await(3, TimeUnit.SECONDS)) {
                        System.out.println("Server response timeout. Please try again.");
                    }
                } catch (InterruptedException e) {
                    System.err.println("Logout interrupted.");
                }
                break;

            case "6":
                exitApplication();
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
        if (!headerShown) {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║ Room: " + padRight(client.getCurrentRoom(), 67) + " ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════════════════╣");
            System.out.println("║ Type your message and press Enter                                         ║");
            System.out.println("║ Commands: /leave (leave room), /quit (exit application)                   ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════════════╝");
            headerShown = true;
        }

        System.out.print("> ");
        String input = scanner.nextLine();

        if (input.equals("/leave")) {
            leaveRoomLatch = new CountDownLatch(1);
            client.leaveRoom();
            try {
                if (!running) {
                    return;
                }
                leaveRoomLatch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting to leave room.");
            }
            return;
        } else if (input.equals("/quit")) {
            exitApplication();
        } else if (!input.trim().isEmpty()) {
            client.sendMessage(input);
            showChatInterface();
        } else {
            showChatInterface();
        }
    }

    /**
     * Pad a string to the right with spaces
     */
    private String padRight(String s, int n) {
        if (s == null) {
            return String.format("%-" + n + "s", "");
        }
        return String.format("%-" + n + "s", s);
    }

    /**
     * Login to the server.
     */
    private void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        authLatch = new CountDownLatch(1);

        client.login(username, password);

        try {
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

        authLatch = new CountDownLatch(1);

        client.register(username, password);

        try {
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

        System.out.println("Joining room automatically...");
        joinRoomLatch = new CountDownLatch(1);
        client.joinRoom(roomName);
        try {
            joinRoomLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting to join room.");
        }
    }

    /**
     * Create a new AI chat room.
     */
    private void createAiRoom() {
        System.out.print("AI Room name: ");
        String roomName = scanner.nextLine();
        client.createAiRoom(roomName, DEFAULT_AI_PROMPT);

        System.out.println("Joining AI room automatically...");
        joinRoomLatch = new CountDownLatch(1);
        client.joinRoom(roomName);
        try {
            joinRoomLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting to join AI room.");
        }
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
                            System.out.println("• " + room);
                        }
                    }
                }
                if (roomListLatch != null) {
                    roomListLatch.countDown();
                }
                //roomListLatch.countDown();
                break;

            case ROOM_CREATED:
                System.out.println("Room created: " + data);
                break;

            case ROOM_EXISTS:
                System.out.println("Room already exists: " + data);
                break;

            case ROOM_NOT_FOUND:
                System.out.println("Room not found: " + data);
                break;

            case ROOM_JOINED:
                System.out.println("Joined room: " + data);
                if (joinRoomLatch != null) {
                    joinRoomLatch.countDown();
                }
                //joinRoomLatch.countDown();
                break;

            case ROOM_LEFT:
                System.out.println("Left room: " + data);
                if (leaveRoomLatch != null) {
                    leaveRoomLatch.countDown();
                }
                break;

            case MESSAGE_RECEIVED:
                if (data instanceof String[] messageData) {
                    String roomName = messageData[0];
                    String messageContent = messageData[1];

                    if (roomName.equals(client.getCurrentRoom())) {
                        System.out.println(messageContent);
                        System.out.print("> ");
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

    /**
     * Exit the application immediately.
     */
    private void exitApplication() {
        running = false;
        if (authLatch != null) {
            authLatch.countDown();
        }
        if (joinRoomLatch != null) {
            joinRoomLatch.countDown();
        }
        if (roomListLatch != null) {
            roomListLatch.countDown();
        }
        if (leaveRoomLatch != null) {
            leaveRoomLatch.countDown();
        }
        //System.exit(0);
    }
}
