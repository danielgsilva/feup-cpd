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
            out.println("Welcome! Please enter your username:");
            username = in.readLine();
            out.println("Enter password:");
            String password = in.readLine();

            if (!AuthService.authenticate(username, password)) {
                out.println("[Server] Login failed. Connection closing.");
                socket.close();
                return;
            }

            out.println("[Server] Login successful!");
            out.println("Enter room name (new or existing):");
            String roomName = in.readLine();
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