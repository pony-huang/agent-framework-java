package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.message.Message;

import java.util.List;
import java.util.Map;

public interface Session {

    String getId();

    List<Message> getMessages();

    void addMessage(Message message);

    void addMessages(List<Message> messages);

    List<Message> getHistory(int limit);

    void clearHistory();

    // Lifecycle
    void start();

    void end();

    boolean isActive();

    // Metadata
    Map<String, Object> getMetadata();

    void setMetadata(String key, Object value);

    Object getMetadata(String key);

    // Configuration
    void setTimeoutMs(long timeoutMs);

    void setMaxMessages(int maxMessages);

    long getTimeoutMs();

    int getMaxMessages();

    long getLastActiveTime();

    void updateLastActiveTime();

    // Fork/Resume support
    /**
     * Create a forked copy of this session.
     * The fork is independent - changes to the fork don't affect the original.
     *
     * @return a new Session that is a copy of this one
     */
    Session fork();

    /**
     * Resume a session from a stored state.
     *
     * @param sessionId the ID of the session to resume
     * @return the resumed session, or null if not found
     */
    static Session resume(String sessionId) {
        // Default implementation - subclasses should override
        return null;
    }

    /**
     * Get the parent session ID if this is a fork.
     *
     * @return parent session ID, or null if not a fork
     */
    default String getParentSessionId() {
        return null;
    }
}
