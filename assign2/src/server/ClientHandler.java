package src.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private String username;
    private Room currentRoom;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            this.out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Welcome! Do you want to (1) Register or (2) Login?");
            String choice = in.readLine();

            if ("1".equals(choice)) {
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

            // Login flow
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
            showHelp();

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/help")) {
                    showHelp();
                } else if (message.startsWith("/join ")) {
                    String newRoomName = message.substring(6).trim();
                    Room newRoom = RoomManager.getRoom(newRoomName);

                    if (currentRoom != null) {
                        currentRoom.leave(this);
                    }

                    currentRoom = newRoom;
                    currentRoom.join(this);
                    out.println("[Server] You have joined room: " + newRoomName);
                } else if (message.equalsIgnoreCase("/quit")) {
                    out.println("[Server] Exiting...");
                    break;
                } else {
                    currentRoom.broadcast(username + ": " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (currentRoom != null) {
                currentRoom.leave(this);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    private void showHelp() {
        out.println("[Server] Available commands:");
        out.println("  /join <roomName> - Change to another room or create a new one");
        out.println("  /help            - Show this help");
        out.println("  /quit            - Leave the chat");
    }

    public String getUsername() {
        return username;
    }
}
