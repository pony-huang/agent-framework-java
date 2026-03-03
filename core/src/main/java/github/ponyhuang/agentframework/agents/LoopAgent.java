package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.InMemoryAgentSession;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * LoopAgent - An agent that supports multi-turn ReAct execution loop.
 * Extends BaseAgent to provide iterative tool calling until task completion
 * or max steps reached.
 */
public class LoopAgent extends BaseAgent {

    private static final int DEFAULT_MAX_STEPS = 10;

    private final ToolExecutor toolExecutor;
    private final int maxSteps;
    private final LoopTerminationHandler terminationHandler;

    protected LoopAgent(Builder builder) {
        super(builder);
        this.toolExecutor = builder.toolExecutor;
        this.maxSteps = builder.maxSteps;
        this.terminationHandler = builder.terminationHandler;
    }

    @Override
    protected AgentSession createSession(Map<String, Object> options) {
        return new InMemoryAgentSession(this);
    }

    @Override
    protected ChatResponse doRun(List<Message> messages, Map<String, Object> options) {
        List<Message> conversationMessages = new ArrayList<>(messages);
        int currentStep = 0;

        while (currentStep < maxSteps) {
            currentStep++;
            LOG.debug("Step {}/{}", currentStep, maxSteps);

            // Build chat request
            ChatCompleteParams params = ChatCompleteParams.builder()
                    .messages(conversationMessages)
                    .tools(toolExecutor != null ? toolExecutor.getToolSchemas() : List.of())
                    .build();

            // Call LLM
            ChatResponse response = client.chat(params);

            // Add assistant message to conversation
            Message assistantMessage = response.getMessage();
            conversationMessages.add(assistantMessage);

            // Check if we have a function call
            if (!response.hasFunctionCall()) {
                // No function call, return the response
                return response;
            }

            // Handle function calls - ReAct loop
            Map<String, Object> functionCall = assistantMessage.getFunctionCall();
            if (functionCall == null) {
                break;
            }

            String functionName = (String) functionCall.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> functionArgs = (Map<String, Object>) functionCall.get("arguments");
            String toolCallId = (String) functionCall.get("id");

            LOG.info("Executing tool: {}", functionName);

            // Execute tool
            Object toolResult;
            try {
                if (toolExecutor != null) {
                    toolResult = toolExecutor.execute(functionName, functionArgs != null ? functionArgs : Collections.emptyMap());
                } else {
                    toolResult = "Error: No tool executor configured";
                }
            } catch (Exception e) {
                LOG.error("Tool execution failed: {}", e.getMessage());
                toolResult = "Error: " + e.getMessage();
            }

            // Check for termination
            if (terminationHandler != null && terminationHandler.shouldTerminate(functionName, functionArgs, toolResult)) {
                LOG.info("Termination handler signaled stop at step {}", currentStep);
                return response;
            }

            // Add tool result message
            String resultStr = toolResult != null ? toolResult.toString() : "null";
            Message toolMessage = Message.tool(toolCallId, functionName, resultStr);
            conversationMessages.add(toolMessage);
        }

        LOG.warn("Max steps reached: {}", maxSteps);

        // Return last response
        return conversationMessages.size() > messages.size()
                ? client.chat(ChatCompleteParams.builder()
                        .messages(conversationMessages)
                        .build())
                : ChatResponse.builder()
                        .choices(List.of(new ChatResponse.Choice(0,
                                Message.assistant("Maximum steps reached without task completion."),
                                "max_tokens")))
                        .build();
    }

    @Override
    protected Flux<ChatResponse> doRunStream(List<Message> messages, Map<String, Object> options) {
        // Delegate to regular run for now
        return Flux.just(doRun(messages, options));
    }

    /**
     * Get the tool executor.
     */
    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }

    /**
     * Get max steps configured.
     */
    public int getMaxSteps() {
        return maxSteps;
    }

    /**
     * Create a new LoopAgent builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Handler for determining when the loop should terminate early.
     */
    @FunctionalInterface
    public interface LoopTerminationHandler {
        /**
         * Determine if the loop should terminate.
         *
         * @param functionName the function that was called
         * @param arguments    the function arguments
         * @param result       the function result
         * @return true if should terminate, false to continue
         */
        boolean shouldTerminate(String functionName, Map<String, Object> arguments, Object result);
    }

    /**
     * Builder for LoopAgent.
     */
    public static class Builder extends BaseAgent.Builder<LoopAgent, Builder> {

        private ToolExecutor toolExecutor;
        private int maxSteps = DEFAULT_MAX_STEPS;
        private LoopTerminationHandler terminationHandler;

        public Builder toolExecutor(ToolExecutor toolExecutor) {
            this.toolExecutor = toolExecutor;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder terminationHandler(LoopTerminationHandler terminationHandler) {
            this.terminationHandler = terminationHandler;
            return this;
        }

        @Override
        public LoopAgent build() {
            return new LoopAgent(this);
        }
    }
}