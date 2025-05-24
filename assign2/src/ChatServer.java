
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main server class for the chat application. Listens for client connections
 * and spawns virtual threads to handle them. Includes fault tolerance features.
 */
public class ChatServer {

    private boolean running;
    private final int port;
    private final AuthenticationService authService;
    private final RoomManager roomManager;
    private final TokenService tokenService;
    private final SessionManager sessionManager;
    private final ScheduledExecutorService scheduler;

    /**
     * Create a new chat server listening on the specified port.
     *
     * @param port The port to listen on
     */
    public ChatServer(int port) {
        this.port = port;
        this.authService = new AuthenticationService();
        this.roomManager = new RoomManager();
        this.tokenService = new TokenService();
        this.sessionManager = new SessionManager();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.running = false;

        // Create default rooms
        this.roomManager.createRoom("library");
        this.roomManager.createRoom("cpd");
        this.roomManager.createRoom("ia");
        this.roomManager.createRoom("cg");
        this.roomManager.createRoom("compiladores");

        // Schedule token cleanup every hour
        this.scheduler.scheduleAtFixedRate(this.tokenService::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Start the server and listen for client connections.
     */
    public void start() {
        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: "
                            + clientSocket.getInetAddress().getHostAddress()
                            + ":" + clientSocket.getPort());

                    // Create a virtual thread to handle this client
                    Thread.startVirtualThread(() -> handleClient(clientSocket));

                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        } finally {
            scheduler.shutdown();
        }
    }

    /**
     * Handle a client connection.
     *
     * @param clientSocket The socket for the client connection
     */
    private void handleClient(Socket clientSocket) {
        try {
            ClientHandler handler = new ClientHandler(clientSocket, authService, roomManager, tokenService, sessionManager);
            handler.handle();
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Ignore close exceptions
            }
        }
    }

    /**
     * Stop the server.
     */
    public void stop() {
        running = false;
        scheduler.shutdownNow();
    }

    /**
     * Main entry point for the server application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // java ChatServer [<port>] 

        int port = 1234; // Default port

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port 1234.");
            }
        }

        ChatServer server = new ChatServer(port);

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            server.stop();
        }));

        server.start();
    }
}
