package src.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class ChatServer {
    public static void main(String[] args) throws IOException {
        int port = 12345;
        var threadFactory = Thread.ofVirtual().factory();
        var executor = Executors.newThreadPerTaskExecutor(threadFactory);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> new ClientHandler(clientSocket).run());
            }
        }
    }
}