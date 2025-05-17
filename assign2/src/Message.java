
/**
 * Represents a chat message.
 */
public class Message {

    private final String sender;
    private final String content;

    /**
     * Create a new message.
     *
     * @param sender The username of the sender
     * @param content The message content
     */
    public Message(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    /**
     * Get the username of the sender.
     *
     * @return The sender's username
     */
    public String getSender() {
        return sender;
    }

    /**
     * Get the message content.
     *
     * @return The message content
     */
    public String getContent() {
        return content;
    }

    /**
     * Format the message in the protocol format.
     *
     * @return The protocol-formatted message
     */
    public String toProtocolString() {
        return sender + " " + content;
    }
}
