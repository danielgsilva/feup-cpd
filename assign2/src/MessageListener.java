
import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;

/**
 * Listens for messages from the server and handles connection loss detection.
 */
public class MessageListener implements Runnable {

    private final ChatClient client;
    private final BufferedReader in;
    private volatile boolean running;

    /**
     * Create a new message listener.
     *
     * @param client The chat client
     * @param in The input stream
     */
    public MessageListener(ChatClient client, BufferedReader in) {
        this.client = client;
        this.in = in;
        this.running = true;
    }

    /**
     * Stop the listener.
     */
    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                if (!running) {
                    break;
                }

                final String msg = message;
                // Process the message on a virtual thread to avoid blocking
                Thread.startVirtualThread(() -> client.handleServerMessage(msg));
            }

            // If we reach here and we were still running, the connection was lost
            if (running) {
                client.handleConnectionLoss();
            }

        } catch (SocketException e) {
            if (running) {
                System.err.println("Socket connection lost: " + e.getMessage());
                client.handleConnectionLoss();
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error reading from server: " + e.getMessage());
                client.handleConnectionLoss();
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("Unexpected error in message listener: " + e.getMessage());
                client.handleConnectionLoss();
            }
        } finally {
            //System.out.println("Message listener stopped");
        }
    }
}
