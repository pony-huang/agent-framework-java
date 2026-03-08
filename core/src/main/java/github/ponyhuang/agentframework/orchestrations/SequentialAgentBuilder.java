package github.ponyhuang.agentframework.orchestrations;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.block.TextBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builder for sequential agent execution.
 * Agents are executed one after another, with each agent's output feeding into the next.
 */
public class SequentialAgentBuilder {

    private final List<Agent> agents = new ArrayList<>();
    private Function<String, String> outputTransformer = Function.identity();

    /**
     * Adds an agent to the sequence.
     *
     * @param agent the agent to add
     * @return this builder
     */
    public SequentialAgentBuilder agent(Agent agent) {
        agents.add(agent);
        return this;
    }

    /**
     * Sets a transformer function for agent outputs.
     *
     * @param transformer the output transformer
     * @return this builder
     */
    public SequentialAgentBuilder outputTransformer(Function<String, String> transformer) {
        this.outputTransformer = transformer;
        return this;
    }

    /**
     * Executes the agents in sequence.
     *
     * @param initialInput the initial input
     * @return the final response
     */
    public ChatResponse execute(String initialInput) {
        String currentInput = initialInput;
        ChatResponse lastResponse = null;

        for (Agent agent : agents) {
            Message userMessage = UserMessage.create(currentInput);
            lastResponse = agent.run(List.of(userMessage));

            // Transform output for next agent
            String output = lastResponse.getMessage() != null
                    ? getMessageText(lastResponse.getMessage())
                    : "";
            currentInput = outputTransformer.apply(output);
        }

        return lastResponse;
    }

    /**
     * Executes the agents in sequence with messages.
     *
     * @param messages the initial messages
     * @return the final response
     */
    public ChatResponse execute(List<Message> messages) {
        List<Message> currentMessages = new ArrayList<>(messages);

        for (Agent agent : agents) {
            ChatResponse response = agent.run(currentMessages);

            if (response.getMessage() != null) {
                currentMessages.add(response.getMessage());
            }
        }

        return currentMessages.size() > messages.size()
                ? createResponseFromMessages(currentMessages.subList(messages.size(), currentMessages.size()))
                : null;
    }

    private ChatResponse createResponseFromMessages(List<Message> messages) {
        if (messages.isEmpty()) return null;
        Message lastMessage = messages.get(messages.size() - 1);
        return ChatResponse.builder()
                .messages(List.of(lastMessage))
                .build();
    }

    private String getMessageText(Message message) {
        if (message == null || message.getBlocks() == null) return null;
        StringBuilder sb = new StringBuilder();
        for (github.ponyhuang.agentframework.types.block.Block block : message.getBlocks()) {
            if (block instanceof TextBlock) {
                sb.append(((TextBlock) block).getText());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Gets the number of agents in the sequence.
     *
     * @return the agent count
     */
    public int getAgentCount() {
        return agents.size();
    }
}
