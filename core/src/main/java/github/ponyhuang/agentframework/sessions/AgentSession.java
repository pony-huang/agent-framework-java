package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for managing an agent's conversation session.
 * Handles message history and state.
 */
public interface AgentSession {

    /**
     * Gets the agent associated with this session.
     *
     * @return the agent
     */
    Agent getAgent();

    /**
     * Gets the session ID.
     *
     * @return the session ID
     */
    String getId();

    /**
     * Gets all messages in the session.
     *
     * @return list of messages
     */
    List<Message> getMessages();

    /**
     * Adds a message to the session.
     *
     * @param message the message to add
     */
    void addMessage(Message message);

    /**
     * Adds multiple messages to the session.
     *
     * @param messages the messages to add
     */
    void addMessages(List<Message> messages);

    /**
     * Gets the message history.
     *
     * @param limit the maximum number of messages to return
     * @return list of messages
     */
    List<Message> getHistory(int limit);

    /**
     * Clears the session history.
     */
    void clearHistory();

    /**
     * Runs the agent with a user message.
     *
     * @param userMessage the user message
     * @return the agent response
     */
    default ChatResponse run(String userMessage) {
        return run(UserMessage.create(userMessage));
    }

    /**
     * Runs the agent with a message.
     *
     * @param message the message
     * @return the agent response
     */
    default ChatResponse run(Message message) {
        addMessage(message);
        List<Message> currentMessages = getMessages();
        Map<String, Object> options = new HashMap<>();

        // Pre-run hooks
        for (ContextProvider provider : getAgent().getContextProviders()) {
            currentMessages = provider.beforeRun(getAgent(), this, currentMessages, options);
        }

        ChatResponse response = getAgent().run(currentMessages);

        // Post-run hooks
        for (ContextProvider provider : getAgent().getContextProviders()) {
            provider.afterRun(getAgent(), this, currentMessages, response, options);
        }

        if (response.getMessage() != null) {
            addMessage(response.getMessage());
        }
        return response;
    }

    /**
     * Runs the agent with streaming.
     *
     * @param userMessage the user message
     * @return flux of message updates
     */
    default Flux<Message> runStream(String userMessage) {
        return runStream(UserMessage.create(userMessage));
    }

    /**
     * Runs the agent with streaming.
     *
     * @param message the message
     * @return flux of message updates
     */
    Flux<Message> runStream(Message message);

    /**
     * Gets session metadata.
     *
     * @return metadata map
     */
    Map<String, Object> getMetadata();

    /**
     * Sets session metadata.
     *
     * @param key the metadata key
     * @param value the metadata value
     */
    void setMetadata(String key, Object value);

    /**
     * Gets a metadata value.
     *
     * @param key the metadata key
     * @return the metadata value, or null if not set
     */
    Object getMetadata(String key);
}
