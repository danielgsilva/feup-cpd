/**
 * Listener for client events.
 */
public interface ChatClientListener {

    /**
     * Called when a client event occurs.
     *
     * @param event The event
     * @param data Event data
     */
    void onEvent(ClientEvent event, Object data);
}