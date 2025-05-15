package src.server;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private String username;
    private Room currentRoom;

    // Mapa para tokens ativos -> sessão
    private static final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();
    private static final long TOKEN_VALIDITY_MILLIS = 24 * 60 * 60 * 1000; // 24 horas

    private static class SessionData {
        String username;
        Room room;
        long expiration;
        ClientHandler clientHandler;
    }

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            this.out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Welcome! If reconnecting, send your token as: TOKEN:<token>");
            out.println("Otherwise, do you want to (1) Register or (2) Login?");
            String line = in.readLine();

            if (line != null && line.startsWith("TOKEN:")) {
                String token = line.substring(6).trim();
                SessionData session = activeSessions.get(token);
                if (session != null && session.expiration > System.currentTimeMillis()) {
                    // Reconexão válida
                    username = session.username;
                    currentRoom = session.room;
                    session.clientHandler = this; // atualiza handler ativo
                    out.println("[Server] Reconnected with token. Welcome back " + username);
                    currentRoom.join(this);
                    messageLoop(in);
                    return;
                } else {
                    out.println("[Server] Invalid or expired token. Please login or register.");
                    out.println("Do you want to (1) Register or (2) Login?");
                    line = in.readLine();
                }
            }

            if ("1".equals(line)) {
                if (!registerFlow(in, out)) return;
            }

            if ("2".equals(line) || "1".equals(line)) {
                if (!loginFlow(in, out)) return;
            } else {
                out.println("[Server] Invalid choice. Closing connection.");
                return;
            }

            messageLoop(in);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (currentRoom != null) {
                currentRoom.leave(this);
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private boolean registerFlow(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Enter desired username:");
        String regUsername = in.readLine();
        out.println("Enter desired password:");
        String regPassword = in.readLine();

        if (regUsername == null || regPassword == null) {
            out.println("[Server] Invalid input. Closing connection.");
            return false;
        }

        if (!AuthService.registerUser(regUsername, regPassword)) {
            out.println("[Server] Username already taken. Closing connection.");
            return false;
        }

        out.println("[Server] Registration successful! Please login now.");
        return true;
    }

    private boolean loginFlow(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Enter username:");
        username = in.readLine();
        out.println("Enter password:");
        String password = in.readLine();

        if (username == null || password == null || !AuthService.authenticate(username, password)) {
            out.println("[Server] Login failed. Closing connection.");
            return false;
        }

        out.println("[Server] Login successful!");
        out.println("Enter room name (new or existing):");
        String roomName = in.readLine();
        if (roomName == null) {
            out.println("[Server] Invalid room name. Closing connection.");
            return false;
        }

        currentRoom = RoomManager.getRoom(roomName);
        currentRoom.join(this);

        // Gerar token e guardar sessão
        String token = UUID.randomUUID().toString();
        SessionData session = new SessionData();
        session.username = username;
        session.room = currentRoom;
        session.expiration = System.currentTimeMillis() + TOKEN_VALIDITY_MILLIS;
        session.clientHandler = this;
        activeSessions.put(token, session);

        out.println("TOKEN:" + token);
        return true;
    }

    private void messageLoop(BufferedReader in) throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            if (message.equalsIgnoreCase("/help")) {
                showHelp();
            } else if (message.startsWith("/join ")) {
                String newRoomName = message.substring(6).trim();
                Room newRoom = RoomManager.getRoom(newRoomName);

                if (currentRoom != null) currentRoom.leave(this);

                currentRoom = newRoom;
                currentRoom.join(this);
                out.println("[Server] You have joined room: " + newRoomName);

                updateSessionRoom();
            } else if (message.equalsIgnoreCase("/members")) {
                Set<String> members = currentRoom.getMemberUsernames();
                out.println("[Server] Members in room '" + currentRoom.getName() + "':");
                for (String member : members) {
                    out.println("  - " + member);
                }
            } else if (message.equalsIgnoreCase("/quit")) {
                out.println("[Server] Exiting...");
                break;
            } else {
                if (currentRoom != null) {
                    currentRoom.broadcast(username + ": " + message);
                } else {
                    out.println("[Server] You are not in a room. Use /join <roomName>.");
                }
            }
        }
    }

    private void updateSessionRoom() {
        activeSessions.forEach((token, session) -> {
            if (session.username.equals(username) && session.clientHandler == this) {
                session.room = currentRoom;
            }
        });
    }

    private void showHelp() {
        out.println("[Server] Available commands:");
        out.println("  /join <roomName> - Go to an existing or create a new one");
        out.println("  /members         - List current members of the room");
        out.println("  /help            - Show this help");
        out.println("  /quit            - Leave the chat");
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public String getUsername() {
        return username;
    }
}
