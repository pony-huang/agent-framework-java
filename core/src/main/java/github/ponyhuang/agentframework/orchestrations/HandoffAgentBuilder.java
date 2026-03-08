package github.ponyhuang.agentframework.orchestrations;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.block.TextBlock;

import java.util.*;
import java.util.function.Function;

/**
 * Builder for handoff agent execution.
 * Agents can explicitly hand off to other agents based on conditions.
 */
public class HandoffAgentBuilder {

    private final Map<String, Agent> agents = new HashMap<>();
    private Agent defaultAgent;
    private Function<ChatResponse, String> handoffDecider = response -> null;

    /**
     * Adds an agent to the handoff pool.
     *
     * @param name  the agent name/ID
     * @param agent the agent
     * @return this builder
     */
    public HandoffAgentBuilder agent(String name, Agent agent) {
        agents.put(name, agent);
        if (defaultAgent == null) {
            defaultAgent = agent;
        }
        return this;
    }

    /**
     * Sets the default agent for handoff.
     *
     * @param agent the default agent
     * @return this builder
     */
    public HandoffAgentBuilder defaultAgent(Agent agent) {
        this.defaultAgent = agent;
        return this;
    }

    /**
     * Sets a function to decide which agent to handoff to.
     *
     * @param decider function that takes a response and returns agent name or null
     * @return this builder
     */
    public HandoffAgentBuilder handoffDecider(Function<ChatResponse, String> decider) {
        this.handoffDecider = decider;
        return this;
    }

    /**
     * Executes the handoff pattern.
     *
     * @param input the initial input
     * @return the final response
     */
    public ChatResponse execute(String input) {
        return execute(input, defaultAgent);
    }

    /**
     * Executes the handoff pattern starting with a specific agent.
     *
     * @param input the initial input
     * @param startAgent the agent to start with
     * @return the final response
     */
    public ChatResponse execute(String input, Agent startAgent) {
        Agent currentAgent = startAgent != null ? startAgent : defaultAgent;
        if (currentAgent == null) {
            throw new IllegalStateException("No agent available for handoff");
        }

        String currentInput = input;
        ChatResponse lastResponse = null;
        int maxHandoffs = 10; // Prevent infinite loops

        for (int i = 0; i < maxHandoffs; i++) {
            // Execute current agent
            Message userMessage = UserMessage.create(currentInput);
            lastResponse = currentAgent.run(List.of(userMessage));

            // Check for handoff
            String targetAgentName = handoffDecider.apply(lastResponse);
            if (targetAgentName == null) {
                // No handoff needed, return current response
                break;
            }

            Agent nextAgent = agents.get(targetAgentName);
            if (nextAgent == null) {
                throw new IllegalStateException("Unknown agent for handoff: " + targetAgentName);
            }

            // Transform output for next agent
            if (lastResponse.getMessage() != null) {
                currentInput = getMessageText(lastResponse.getMessage());
            }
            currentAgent = nextAgent;
        }

        return lastResponse;
    }

    private static String getMessageText(Message message) {
        if (message.getBlocks() == null) return null;
        StringBuilder sb = new StringBuilder();
        for (github.ponyhuang.agentframework.types.block.Block block : message.getBlocks()) {
            if (block instanceof TextBlock) {
                sb.append(((TextBlock) block).getText());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Creates a simple handoff decider based on response content.
     *
     * @param keywordToAgentMap map of keywords to agent names
     * @return a handoff decider function
     */
    public static Function<ChatResponse, String> createKeywordDecider(Map<String, String> keywordToAgentMap) {
        return response -> {
            if (response == null || response.getMessage() == null) {
                return null;
            }
            String text = getMessageText(response.getMessage());
            if (text == null) return null;
            text = text.toLowerCase();
            for (Map.Entry<String, String> entry : keywordToAgentMap.entrySet()) {
                if (text.contains(entry.getKey().toLowerCase())) {
                    return entry.getValue();
                }
            }
            return null;
        };
    }

    /**
     * Gets an agent by name.
     *
     * @param name the agent name
     * @return the agent, or null if not found
     */
    public Agent getAgent(String name) {
        return agents.get(name);
    }

    /**
     * Gets all agent names.
     *
     * @return set of agent names
     */
    public Set<String> getAgentNames() {
        return agents.keySet();
    }
}
