
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private ServerSocket serverSocket;
    private int noSensors;
    private Sensor[] sensors;
    private static final int SIMULATED_DELAY = 2000; // 2 seconds delay for simulation

    public static void main(String[] args) {
        // java Server <port> <no_sensors>
        if (args.length != 2) {
            System.out.println("Usage: java Server <port> <no_sensors>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int noSensors = Integer.parseInt(args[1]);

        try {
            Server server = new Server(port, noSensors);
            server.run();
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }

    public Server(int port, int noSensors) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.noSensors = noSensors;
        this.sensors = new Sensor[noSensors];

        // Initialize sensors
        for (int i = 0; i < noSensors; i++) {
            sensors[i] = new Sensor(i);
        }

        System.out.println("TCP server running on port " + port);
        System.out.println("Number of sensors: " + noSensors);
        System.out.println("Waiting for connections...");
    }

    public void run() {
        try (ServerSocket server = this.serverSocket) {
            while (true) {
                Socket clientSocket = server.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                System.out.println("Creating a new thread for the client...");

                // Create and start a new virtual thread for each client
                Thread.ofVirtual().name("client-" + clientSocket.getPort()).start(() -> {
                    handleClient(clientSocket);
                });
            }
        } catch (IOException e) {
            System.out.println("Error accepting client connection: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                clientSocket;
                // Configure input and output streams
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
            // Read message from client
            String message = in.readLine(); // expects a message in the format: <op> <id> [<val>]
            String threadName = Thread.currentThread().getName();
            System.out.println(threadName + " - Received message: " + message);

            // Process the message
            // <op> <id> [<val>]
            // where <op> is either PUT or GET
            // and <id> is the sensor ID
            // and <val> is the value to be added (only for PUT)
            if (message != null) {
                // Emulate a delay for processing
                try {
                    Thread.sleep(SIMULATED_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String[] parts = message.split(" ");
                String operation = parts[0];

                if (operation.equalsIgnoreCase("PUT")) {
                    // PUT operation
                    int sensorId = Integer.parseInt(parts[1]);
                    float value = Float.parseFloat(parts[2]);

                    if (sensorId >= 0 && sensorId < noSensors) {
                        sensors[sensorId].addReading(value);
                        System.out.println(threadName + " - Added reading " + value + " to sensor " + sensorId);
                    } else {
                        System.out.println(threadName + " - Invalid sensor ID: " + sensorId);
                    }

                } else if (operation.equalsIgnoreCase("GET")) {
                    // GET operation
                    int sensorId = Integer.parseInt(parts[1]);

                    if (sensorId >= 0 && sensorId < noSensors) {
                        float average = sensors[sensorId].getAverage();

                        // Send the average back to the client
                        String response = "AVG " + sensorId + " " + average;
                        out.println(response);
                        System.out.println(threadName + " - Sent response: " + response);
                    } else {
                        out.println("ERROR: ID de sensor inválido");
                        System.out.println(threadName + " - Invalid sensor ID: " + sensorId);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(Thread.currentThread().getName() + " - Error: " + e.getMessage());
        }
    }

    // Sensor class to hold sensor data
    private class Sensor {

        private int id;
        private List<Float> readings;

        public Sensor(int id) {
            this.id = id;
            // Use a synchronized list to handle concurrent access
            this.readings = Collections.synchronizedList(new ArrayList<>());
        }

        public synchronized void addReading(float value) {
            readings.add(value);
        }

        public synchronized float getAverage() {
            if (readings.isEmpty()) {
                return 0.0f;
            }

            float sum = 0.0f;
            for (float reading : readings) {
                sum += reading;
            }

            return sum / readings.size();
        }
    }
}
