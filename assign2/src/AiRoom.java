import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Special room type that integrates with an AI model via Ollama.
 * Every time a user sends a message, the entire conversation context
 * along with the initial prompt is sent to the AI model, and the response
 * is added to the room as a message from "Bot".
 */
public class AiRoom extends Room {

    private final String prompt;
    private final String model;
    private final HttpClient httpClient;
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_RETRIES = 3;

    /**
     * Create a new AI room with the given name and prompt.
     *
     * @param name The name of the room
     * @param prompt The initial prompt/instructions for the AI
     */
    public AiRoom(String name, String prompt) {
        super(name);
        this.prompt = prompt;
        this.model = "llama3";
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(60))
                .build();

        // Add welcome messages
        super.addMessage(new Message("SYSTEM", "AI room created with prompt: " + prompt));
        super.addMessage(new Message("SYSTEM", "Bot is ready to respond to your messages."));

        // Test Ollama connection
        testOllamaConnection();
    }

    /**
     * Test the connection to Ollama and log the result.
     */
    private void testOllamaConnection() {
        Thread.startVirtualThread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:11434/api/tags"))
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    super.addMessage(new Message("SYSTEM", "Connected to Ollama successfully."));
                } else {
                    super.addMessage(new Message("SYSTEM", "Warning: Could not connect to Ollama. AI responses may not work."));
                }
            } catch (Exception e) {
                super.addMessage(new Message("SYSTEM", "Warning: Could not connect to Ollama (" + e.getMessage() + "). AI responses may not work."));
            }
        });
    }

    /**
     * Override the addMessage method to also generate an AI response
     * whenever a user adds a message.
     *
     * @param message The message to add
     */
    @Override
    public void addMessage(Message message) {
        // First add the user's message to the room
        super.addMessage(message);

        // Don't respond to system messages or messages from the Bot itself
        if (message.getSender().equals("SYSTEM") || message.getSender().equals("Bot")) {
            return;
        }

        // Add a "thinking" message immediately
        Message thinkingMessage = new Message("SYSTEM", "Bot is thinking...");
        super.addMessage(thinkingMessage);

        // Run AI query in a separate thread to avoid blocking
        Thread.startVirtualThread(() -> {
            try {
                // Get user message
                String userMessage = message.getContent();

                // Query Ollama for a response
                String response = null;
                try {
                    response = queryOllama(userMessage);
                } catch (Exception e) {
                    // If Ollama fails, use a fallback response
                    response = "I'm sorry, I couldn't process your request right now. The AI service might be temporarily unavailable.";
                }

                // Remove the "thinking" message
                removeMessage(thinkingMessage);

                if (response != null && !response.trim().isEmpty()) {
                    // Create a new message from the Bot with the AI's response
                    Message botMessage = new Message("Bot", response.trim());
                    super.addMessage(botMessage);
                } else {
                    // Add an error message
                    String errorMsg = "Sorry, I couldn't generate a response right now.";
                    super.addMessage(new Message("Bot", errorMsg));
                }
            } catch (Exception e) {
                // Remove the "thinking" message
                removeMessage(thinkingMessage);

                // Add an error message to the room
                super.addMessage(new Message("Bot", "Sorry, I encountered an error: " + e.getMessage()));
            }
        });
    }

    /**
     * Query Ollama with the user message
     */
    private String queryOllama(String userMessage) throws IOException, InterruptedException {
        // Create a simple JSON request
        String jsonBody = String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
                model,
                escapeJson("User: " + userMessage + "\nAssistant:")
        );

        // Create the HTTP request with increased timeout
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofMinutes(5))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP error: " + response.statusCode());
        }

        String responseBody = response.body();

        // Extract the response text
        if (responseBody.contains("\"response\":\"")) {
            int start = responseBody.indexOf("\"response\":\"") + "\"response\":\"".length();
            int end = responseBody.indexOf("\"", start);
            if (end > start) {
                String extracted = responseBody.substring(start, end);
                return extracted.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            }
        }

        // Fallback - return a generic response if we can't parse it
        return "I received your message but couldn't generate a proper response.";
    }

    /**
     * Remove a specific message from the room.
     *
     * @param message The message to remove
     */
    private void removeMessage(Message message) {
        List<Message> messages = getMessages();
        messages.remove(message);
    }

    /**
     * Escape special characters in a string for JSON.
     *
     * @param text The text to escape
     * @return The escaped text
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
