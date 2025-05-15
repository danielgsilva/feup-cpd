package src.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private String username;
    private Room currentRoom;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("Welcome! Do you want to (1) Register or (2) Login?");
            String choice = in.readLine();

            if ("1".equals(choice)) {
                // Registo
                out.println("Enter desired username:");
                String regUsername = in.readLine();
                out.println("Enter desired password:");
                String regPassword = in.readLine();

                if (regUsername == null || regPassword == null) {
                    out.println("[Server] Invalid input. Closing connection.");
                    socket.close();
                    return;
                }

                if (AuthService.registerUser(regUsername, regPassword)) {
                    out.println("[Server] Registration successful! Please login now.");
                } else {
                    out.println("[Server] Username already taken. Connection closing.");
                    socket.close();
                    return;
                }
            }

            // Login
            out.println("Enter username:");
            username = in.readLine();
            out.println("Enter password:");
            String password = in.readLine();

            if (username == null || password == null || !AuthService.authenticate(username, password)) {
                out.println("[Server] Login failed. Connection closing.");
                socket.close();
                return;
            }

            out.println("[Server] Login successful!");
            out.println("Enter room name (new or existing):");
            String roomName = in.readLine();
            if (roomName == null) {
                out.println("[Server] Invalid room name. Connection closing.");
                socket.close();
                return;
            }
            currentRoom = RoomManager.getRoom(roomName);
            currentRoom.join(this);

            String message;
            while ((message = in.readLine()) != null) {
                currentRoom.broadcast(username + ": " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (currentRoom != null) {
                currentRoom.leave(this);
            }
        }
    }

    public void sendMessage(String msg) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
}
