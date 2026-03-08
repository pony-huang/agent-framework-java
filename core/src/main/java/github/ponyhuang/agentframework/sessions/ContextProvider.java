package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.message.Message;

import java.util.List;
import java.util.Map;

/**
 * Interface for providing context to the agent.
 * Called before and after agent runs.
 */
public interface ContextProvider {

    /**
     * Called before the agent runs.
     * Allows adding messages or modifying the context.
     *
     * @param agent   the agent
     * @param session the current session
     * @param messages the current messages
     * @param options the options
     * @return modified messages to use
     */
    default List<Message> beforeRun(Object agent, AgentSession session, List<Message> messages, Map<String, Object> options) {
        return messages;
    }

    /**
     * Called after the agent runs.
     * Allows processing the response or updating state.
     *
     * @param agent    the agent
     * @param session  the current session
     * @param messages all messages including response
     * @param response the agent response
     * @param options  the options
     */
    default void afterRun(Object agent, AgentSession session, List<Message> messages, Object response, Map<String, Object> options) {
        // Default: no action
    }

    /**
     * Gets the name of this provider.
     *
     * @return the provider name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
