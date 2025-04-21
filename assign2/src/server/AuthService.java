package src.server;


import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static final Map<String, String> users = new HashMap<>();

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("[AuthService] Could not load users.txt");
        }
    }

    public static boolean authenticate(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }
}