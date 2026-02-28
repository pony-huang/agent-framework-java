package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.Message;

import java.util.List;

/**
 * Interface for providing conversation history.
 */
public interface HistoryProvider {

    /**
     * Gets messages from history.
     *
     * @param limit the maximum number of messages to return
     * @return list of messages
     */
    List<Message> getMessages(int limit);

    /**
     * Adds a message to history.
     *
     * @param message the message to add
     */
    void addMessage(Message message);

    /**
     * Adds multiple messages to history.
     *
     * @param messages the messages to add
     */
    void addMessages(List<Message> messages);

    /**
     * Clears the history.
     */
    void clear();

    /**
     * Gets the total number of messages in history.
     *
     * @return the message count
     */
    int getMessageCount();
}
