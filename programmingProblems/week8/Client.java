import java.net.*;
import java.io.*;

public class Client {
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_WAIT_TIME = 1000; // 1 second
    
    public static void main(String[] args) {
        // java Client <addr> <port> <op> <id> [<val>]
        if (args.length < 4 || (args.length < 5 && args[2].equalsIgnoreCase("put"))) {
            System.out.println("Usage: java Client <addr> <port> <op> <id> [<val>]");
            return;
        }
        
        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String operation = args[2];
        int sensorId = Integer.parseInt(args[3]);
        
        if (operation.equalsIgnoreCase("GET")) {
            // GET operation
            performGet(serverAddress, serverPort, sensorId);

        } else if (operation.equalsIgnoreCase("PUT")) {
            // PUT operation
            
            float value = Float.parseFloat(args[4]);
            performPut(serverAddress, serverPort, sensorId, value);
            
        } else {
            System.out.println("Invalid operation. Use GET or PUT.");
        }
    }
    
    private static void performGet(String serverAddress, int serverPort, int sensorId) {
        int retries = 0;
        int waitTime = INITIAL_WAIT_TIME;
        boolean connected = false;
        
        while (retries < MAX_RETRIES && !connected) {
            try {
                // Create socket and connect to the server
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddress, serverPort), 3000); // 3 seconds timeout
                connected = true;
                
                // Configure input and output streams
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Send GET command
                String message = "GET " + sensorId;
                out.println(message);
                System.out.println("Sent GET command for sensor " + sensorId);
                
                // Read response from server
                // Response format: AVG <id> <avg>
                // where <id> is the sensor ID and <avg> is the average value
                String response = in.readLine();
                System.out.println("Received response: " + response);
                
                if (response != null && response.startsWith("AVG")) {
                    String[] parts = response.split(" ");
                    if (parts.length >= 3 && Integer.parseInt(parts[1]) == sensorId) {
                        float average = Float.parseFloat(parts[2]);
                        System.out.println("Average value for sensor " + sensorId + ": " + average);
                    }
                }
                
                // Close connection
                socket.close();
                
            } catch (IOException e) {
                retries++;
                System.out.println("Attempt " + retries + " failed: " + e.getMessage());
                
                if (retries < MAX_RETRIES) {
                    try {
                        System.out.println("Waiting " + waitTime + "ms before retrying...");
                        Thread.sleep(waitTime);
                        // Exponential back-off: double the wait time for each retry
                        waitTime *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.out.println("Could not connect to the server after " + MAX_RETRIES + " attempts.");
                }
            }
        }
    }
    
    private static void performPut(String serverAddress, int serverPort, int sensorId, float value) {
        int retries = 0;
        int waitTime = INITIAL_WAIT_TIME;
        boolean connected = false;
        
        while (retries < MAX_RETRIES && !connected) {
            try {
                // Create socket and connect to the server
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddress, serverPort), 3000); // 3 seconds timeout
                connected = true;
                
                // Configure output stream
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                // Send PUT command
                String message = "PUT " + sensorId + " " + value;
                out.println(message);
                System.out.println("Sent PUT command for sensor " + sensorId + " with value " + value);
                
                // Close connection
                socket.close();
                
                // Wait for a second before the next operation
                Thread.sleep(1000);
                
            } catch (IOException e) {
                retries++;
                System.out.println("Attempt " + retries + " failed: " + e.getMessage());;
                
                if (retries < MAX_RETRIES) {
                    try {
                        System.out.println("Waiting " + waitTime + "ms before retrying...");
                        Thread.sleep(waitTime);
                        // Exponential back-off: double the wait time for each retry
                        waitTime *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.out.println("Could not connect to the server after " + MAX_RETRIES + " attempts.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}