package src.client;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            Thread listener = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println(serverMsg);
                    }
                    System.out.println("[Client] Server closed the connection.");
                    System.exit(0); // encerra o cliente completamente
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            listener.start();

            String input;
            while ((input = userInput.readLine()) != null) {
                out.println(input);
                /*if (input.equalsIgnoreCase("/quit")) {
                    break;
                }*/
            }
        }
    }
}