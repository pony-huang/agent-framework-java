package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.HookExecutor;
import github.ponyhuang.agentframework.middleware.AgentMiddleware;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.ContextProvider;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Core interface for Agent implementations.
 * An Agent combines a ChatClient with tools to handle user requests.
 */
public interface Agent {

    /**
     * Gets the name of this agent.
     *
     * @return the agent name
     */
    String getName();

    /**
     * Gets the instructions for this agent.
     *
     * @return the instructions
     */
    String getInstructions();

    /**
     * Gets the chat client used by this agent.
     *
     * @return the chat client
     */
    ChatClient getClient();

    /**
     * Runs the agent with the given input.
     *
     * @param messages input messages
     * @return the agent response
     */
    default ChatResponse run(List<Message> messages) {
        return run(messages, null);
    }

    /**
     * Runs the agent with the given input and options.
     *
     * @param messages input messages
     * @param options  additional options
     * @return the agent response
     */
    ChatResponse run(List<Message> messages, Map<String, Object> options);

    /**
     * Runs the agent with streaming.
     *
     * @param messages input messages
     * @return a Flux of response updates
     */
    default Flux<ChatResponse> runStream(List<Message> messages) {
        return runStream(messages, null);
    }

    /**
     * Runs the agent with streaming and options.
     *
     * @param messages input messages
     * @param options  additional options
     * @return a Flux of response updates
     */
    Flux<ChatResponse> runStream(List<Message> messages, Map<String, Object> options);

    /**
     * Creates a new session for this agent.
     *
     * @return a new session
     */
    AgentSession createSession();

    /**
     * Gets all available tool definitions for this agent.
     *
     * @return list of tool definitions
     */
    List<Map<String, Object>> getTools();

    /**
     * Adds a tool to this agent.
     *
     * @param tool the tool definition
     * @return this agent for chaining
     */
    Agent addTool(Map<String, Object> tool);

    /**
     * Removes a tool from this agent.
     *
     * @param toolName the name of the tool to remove
     * @return this agent for chaining
     */
    Agent removeTool(String toolName);

    /**
     * Gets all context providers for this agent.
     *
     * @return list of context providers
     */
    List<ContextProvider> getContextProviders();

    /**
     * Adds a context provider to this agent.
     *
     * @param provider the context provider
     * @return this agent for chaining
     */
    Agent addContextProvider(ContextProvider provider);

    /**
     * Gets all middleware for this agent.
     *
     * @return list of middleware
     */
    List<AgentMiddleware> getMiddlewares();

    /**
     * Adds a middleware to this agent.
     *
     * @param middleware the middleware
     * @return this agent for chaining
     */
    Agent addMiddleware(AgentMiddleware middleware);

    /**
     * Gets the hook executor for this agent.
     *
     * @return the hook executor, or null if not configured
     */
    default HookExecutor getHookExecutor() {
        return null;
    }
}
