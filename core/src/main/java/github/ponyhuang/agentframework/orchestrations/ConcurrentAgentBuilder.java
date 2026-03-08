package github.ponyhuang.agentframework.orchestrations;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.block.TextBlock;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Builder for concurrent agent execution.
 * Multiple agents execute in parallel, and results are aggregated.
 */
public class ConcurrentAgentBuilder {

    private final List<Agent> agents = new ArrayList<>();
    private boolean waitForAll = true;

    /**
     * Adds an agent to execute concurrently.
     *
     * @param agent the agent to add
     * @return this builder
     */
    public ConcurrentAgentBuilder agent(Agent agent) {
        agents.add(agent);
        return this;
    }

    /**
     * Sets whether to wait for all agents or just the first one.
     *
     * @param waitForAll true to wait for all, false for first result
     * @return this builder
     */
    public ConcurrentAgentBuilder waitForAll(boolean waitForAll) {
        this.waitForAll = waitForAll;
        return this;
    }

    /**
     * Executes all agents concurrently.
     *
     * @param input the input message
     * @return list of responses
     */
    public List<ChatResponse> execute(String input) {
        Message message = UserMessage.create(input);
        return execute(List.of(message));
    }

    /**
     * Executes all agents concurrently with messages.
     *
     * @param messages the input messages
     * @return list of responses
     */
    public List<ChatResponse> execute(List<Message> messages) {
        List<CompletableFuture<ChatResponse>> futures = agents.stream()
                .map(agent -> CompletableFuture.supplyAsync(() -> {
                    List<Message> collectedMessages = agent.runStream(messages).collectList().block();
                    return ChatResponse.builder()
                            .messages(collectedMessages)
                            .build();
                }))
                .collect(Collectors.toList());

        if (waitForAll) {
            // Wait for all to complete
            return futures.stream()
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (InterruptedException | ExecutionException e) {
                            return null;
                        }
                    })
                    .filter(r -> r != null)
                    .collect(Collectors.toList());
        } else {
            // Wait for first one
            CompletableFuture<Object> first = CompletableFuture.anyOf(
                    futures.toArray(new CompletableFuture[0])
            );
            try {
                ChatResponse result = (ChatResponse) first.get();
                return Arrays.asList(result);
            } catch (InterruptedException | ExecutionException e) {
                return new ArrayList<>();
            }
        }
    }

    /**
     * Aggregates responses into a single response.
     *
     * @param responses list of responses
     * @return aggregated response
     */
    public ChatResponse aggregate(List<ChatResponse> responses) {
        if (responses.isEmpty()) return null;
        if (responses.size() == 1) return responses.get(0);

        // Concatenate all text outputs
        StringBuilder aggregated = new StringBuilder();
        for (ChatResponse response : responses) {
            if (response.getMessage() != null) {
                String text = getMessageText(response.getMessage());
                if (text != null) {
                    aggregated.append(text).append("\n");
                }
            }
        }

        Message aggregatedMessage = AssistantMessage.create(aggregated.toString().trim());
        return ChatResponse.builder()
                .messages(List.of(aggregatedMessage))
                .build();
    }

    private String getMessageText(Message message) {
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
     * Gets the number of agents.
     *
     * @return the agent count
     */
    public int getAgentCount() {
        return agents.size();
    }
}
