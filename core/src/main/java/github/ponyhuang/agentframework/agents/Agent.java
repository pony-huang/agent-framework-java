package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.types.message.Message;
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
     * Runs the agent with streaming.
     *
     * @param messages input messages
     * @return a Flux of message updates
     */
    default Flux<Message> runStream(List<Message> messages) {
        return runStream(messages, null);
    }

    /**
     * Runs the agent with streaming and options.
     *
     * @param messages input messages
     * @param options  additional options
     * @return a Flux of message updates
     */
    Flux<Message> runStream(List<Message> messages, Map<String, Object> options);


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

}
