package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.InMemoryAgentSession;
import github.ponyhuang.agentframework.sessions.SessionOptions;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.message.ResultMessage;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * LoopAgent - An agent that supports multi-turn ReAct execution loop.
 * Extends BaseAgent to provide iterative tool calling until task completion
 * or max steps reached.
 *
 * <p>LoopAgent includes a built-in "task_done" tool that the LLM can call to signal
 * task completion. When called, the loop will terminate and return the result.
 * The result can be either a simple string or a structured result passed to task_done.
 *
 * <p>Example usage:
 * <pre>
 * LoopAgent agent = LoopAgent.builder()
 *     .client(chatClient)
 *     .tool(new MyTools())
 *     .build();
 * </pre>
 */
public class LoopAgent extends BaseAgent {

    /**
     * Default tool name for signaling task completion.
     */
    public static final String TASK_DONE_TOOL_NAME = "task_done";

    private static final int DEFAULT_MAX_STEPS = 10;

    private final ToolExecutor toolExecutor;
    private final int maxSteps;
    private final LoopTerminationHandler terminationHandler;
    private final boolean useBuiltinTaskDone;

    protected LoopAgent(Builder builder) {
        super(builder);
        this.toolExecutor = builder.toolExecutor;
        this.maxSteps = builder.maxSteps;
        this.terminationHandler = builder.terminationHandler;
        this.useBuiltinTaskDone = builder.useBuiltinTaskDone;
    }

    @Override
    public AgentSession createSession(SessionOptions options) {
        return new InMemoryAgentSession(this, options);
    }

    @Override
    protected Flux<Message> doRun(List<Message> messages, Map<String, Object> options) {
        List<Message> conversationMessages = new ArrayList<>(messages);
        int currentStep = 0;

        while (currentStep < maxSteps) {
            currentStep++;
            LOG.debug("Step {}/{}", currentStep, maxSteps);

            // Build chat request - merge tool executor schemas with builder tools
            List<Map<String, Object>> toolSchemas = new ArrayList<>();

            // Add schemas from tool executor
            if (toolExecutor != null) {
                toolSchemas.addAll(toolExecutor.getToolSchemas());
            }

            // Add schemas from builder (includes built-in task_done)
            List<Map<String, Object>> builderTools = getTools();
            if (builderTools != null) {
                for (Map<String, Object> schema : builderTools) {
                    if (!toolSchemas.contains(schema)) {
                        toolSchemas.add(schema);
                    }
                }
            }

            ChatCompleteParams params = ChatCompleteParams.builder()
                    .messages(conversationMessages)
                    .tools(toolSchemas)
                    .build();

            // Call LLM
            ChatResponse response = client.chat(params);

            // Add assistant message to conversation
            Message assistantMessage = response.getMessage();
            conversationMessages.add(assistantMessage);

            // Check if we have a function call
            if (!response.hasFunctionCall()) {
                // No function call, return the response as Flux
                return Flux.fromIterable(conversationMessages);
            }

            // Handle function calls - ReAct loop
            List<ToolUseBlock> toolCalls = response.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                break;
            }

            ToolUseBlock toolCall = toolCalls.get(0);
            String functionName = toolCall.getName();
            Map<String, Object> functionArgs = toolCall.getInput();
            String toolCallId = toolCall.getId();

            LOG.info("Executing tool: {}", functionName);

            // Handle built-in task_done tool
            if (useBuiltinTaskDone && TASK_DONE_TOOL_NAME.equals(functionName)) {
                LOG.info("Task completed at step {} with result: {}", currentStep, functionArgs);
                // Create an assistant message with the final result
                String finalResult = functionArgs != null && functionArgs.containsKey("result")
                        ? functionArgs.get("result").toString()
                        : "Task completed successfully";
                Message completionMessage = AssistantMessage.create(finalResult);
                conversationMessages.add(completionMessage);
                return Flux.fromIterable(conversationMessages);
            }

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
                return Flux.fromIterable(conversationMessages);
            }

            // Check for built-in task_done tool when terminationHandler is null
            if (useBuiltinTaskDone && terminationHandler == null && TASK_DONE_TOOL_NAME.equals(functionName)) {
                LOG.info("Task completed at step {} with result: {}", currentStep, toolResult);
                String finalResult = toolResult != null ? toolResult.toString() : "Task completed successfully";
                Message completionMessage = AssistantMessage.create(finalResult);
                conversationMessages.add(completionMessage);
                return Flux.fromIterable(conversationMessages);
            }

            // Add tool result message
            String resultStr = toolResult != null ? toolResult.toString() : "null";
            Message toolMessage = ResultMessage.create(toolCallId, resultStr);
            conversationMessages.add(toolMessage);
        }

        LOG.warn("Max steps reached: {}", maxSteps);

        // Return last response
        if (conversationMessages.size() > messages.size()) {
            ChatResponse finalResponse = client.chat(ChatCompleteParams.builder()
                    .messages(conversationMessages)
                    .build());
            List<Message> allMessages = new ArrayList<>(conversationMessages);
            if (finalResponse.getMessage() != null) {
                allMessages.add(finalResponse.getMessage());
            }
            return Flux.fromIterable(allMessages);
        }

        Message maxStepsMessage = AssistantMessage.create("Maximum steps reached without task completion.");
        return Flux.just(maxStepsMessage);
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
        private boolean useBuiltinTaskDone = true; // Default enabled

        /**
         * Adds a tool to the agent by registering an instance with @Tool annotated methods.
         * This is a simplified alternative to using ToolExecutor directly.
         *
         * @param toolInstance the object containing @Tool annotated methods
         * @return this builder
         */
        public Builder tool(Object toolInstance) {
            if (toolInstance != null) {
                if (this.toolExecutor == null) {
                    this.toolExecutor = new ToolExecutor();
                }
                this.toolExecutor.registerAnnotated(toolInstance);
                // Add tool schema to the tools list
                for (Map<String, Object> schema : this.toolExecutor.getToolSchemas()) {
                    if (tools == null) {
                        tools = new ArrayList<>();
                    }
                    // Avoid duplicates
                    if (!tools.contains(schema)) {
                        tools.add(schema);
                    }
                }
            }
            return this;
        }

        /**
         * Adds multiple tools to the agent by registering instances with @Tool annotated methods.
         *
         * @param toolInstances the objects containing @Tool annotated methods
         * @return this builder
         */
        public Builder tools(Object... toolInstances) {
            if (toolInstances != null) {
                for (Object toolInstance : toolInstances) {
                    tool(toolInstance);
                }
            }
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

        /**
         * Enable or disable the built-in task_done tool for signaling task completion.
         * Default is enabled.
         *
         * @param useBuiltinTaskDone true to enable, false to disable
         * @return this builder
         */
        public Builder useBuiltinTaskDone(boolean useBuiltinTaskDone) {
            this.useBuiltinTaskDone = useBuiltinTaskDone;
            return this;
        }

        @Override
        public LoopAgent build() {
            // Add built-in task_done tool schema if enabled
            if (useBuiltinTaskDone) {
                addBuiltinTaskDoneTool();
            }
            return new LoopAgent(this);
        }

        /**
         * Adds the built-in task_done tool schema to the tools list.
         */
        private void addBuiltinTaskDoneTool() {
            if (tools == null) {
                tools = new ArrayList<>();
            }

            // Check if task_done already exists
            boolean exists = tools.stream()
                    .anyMatch(schema -> TASK_DONE_TOOL_NAME.equals(schema.get("name")));

            if (!exists) {
                Map<String, Object> taskDoneSchema = new LinkedHashMap<>();
                taskDoneSchema.put("type", "function");
                taskDoneSchema.put("name", TASK_DONE_TOOL_NAME);
                taskDoneSchema.put("description", "Signals that the task has been completed. Call this tool when the user task is done. "
                        + "The agent should pass the final result or summary as the 'result' parameter.");

                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("type", "object");
                parameters.put("properties", Map.of(
                        "result", Map.of(
                                "type", "string",
                                "description", "The final result or summary of the completed task"
                        )
                ));
                parameters.put("required", List.of("result"));
                taskDoneSchema.put("parameters", parameters);

                tools.add(taskDoneSchema);
                LOG.debug("Built-in task_done tool added to agent");
            }
        }
    }
}