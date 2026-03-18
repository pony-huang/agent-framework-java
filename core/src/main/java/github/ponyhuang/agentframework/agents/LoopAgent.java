package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.tools.builtins.FileSystemTools;
import github.ponyhuang.agentframework.tools.builtins.SystemTools;
import github.ponyhuang.agentframework.tools.builtins.TaskDoneTool;
import github.ponyhuang.agentframework.tools.builtins.TaskTools;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.ResultMessage;
import github.ponyhuang.agentframework.types.message.SystemMessage;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static final String DEFAULT_SYSTEM_PROMPT = """
            You are Code Agent, an AI-powered software engineering assistant.
            
            You are helpful, harmless, and honest. Your goal is to assist users with software development tasks
            including writing code, fixing bugs, refactoring, and performing code reviews.
            
            ## Available Tools
            
            You have access to the following tools to accomplish your tasks:
            
            ### File Operations
            - **read**: Read file contents
            - **readRange**: Read specific line range from a file
            - **write**: Create or overwrite a file
            - **edit**: Replace text in a file
            - **glob**: Find files matching a pattern
            
            ### Shell Commands
            - **bash**: Execute shell commands
            - **bashWithTimeout**: Execute with timeout
            - **bashDetailed**: Execute and get detailed result
            
            ### Task Management
            - **create**: Create a new task
            - **createWithOptions**: Create task with full options
            - **update**: Update an existing task
            - **list**: List all tasks
            - **get**: Get a task by ID
            - **delete**: Delete a task
            
            ### System
            - **askUser**: Ask questions to the user
            - **planMode**: Enter planning mode
            
            ### Completion
            - **task_done**: Signal task completion when the work is finished
            
            ## Working Directory
            
            All file operations and shell commands operate relative to the working directory specified by the user.
            
            ## Guidelines
            
            1. Always verify your changes work correctly before marking a task as complete
            2. Use appropriate tools to explore the codebase before making changes
            3. Provide clear, actionable feedback to users
            4. When unsure, ask clarifying questions
            5. Follow best practices for code quality and security
            
            ## Task Completion
            
            When you believe the task is complete, call the task_done tool with a summary of what was accomplished.
            If there are remaining issues or the task cannot be completed, explain why.
            """;

    private static final int DEFAULT_MAX_STEPS = 10;

    private final ToolExecutor toolExecutor;
    private final int maxSteps;
    private final CostTracker costTracker;
    private final Set<String> allowedTools;
    private final Set<String> disallowedTools;
    private final String fallbackModel;
    private final PermissionMode permissionMode;

    protected LoopAgent(Builder builder) {
        super(builder);
        this.toolExecutor = builder.toolExecutor;
        this.maxSteps = builder.maxSteps;
        this.costTracker = new CostTracker(builder.maxBudgetUsd);
        this.allowedTools = builder.allowedTools;
        this.disallowedTools = builder.disallowedTools;
        this.fallbackModel = builder.fallbackModel;
        this.permissionMode = builder.permissionMode;
    }

    @Override
    protected Flux<Message> doRun(List<Message> messages, Map<String, Object> options) {
        Message systemMessage = SystemMessage.create().withText(DEFAULT_SYSTEM_PROMPT);
        List<Message> conversationMessages = new ArrayList<>(messages);
        conversationMessages.add(0, systemMessage);

        // State for the reactive loop
        AtomicInteger currentStep = new AtomicInteger(0);

        return Flux.generate(
                () -> conversationMessages,
                (convMessages, sink) -> {
                    int step = currentStep.incrementAndGet();

                    if (step > maxSteps) {
                        // Max steps reached - emit final summary request
                        LOG.warn("Max steps reached: {}", maxSteps);
                        String summaryPrompt = """
                                The maximum number of steps has been reached. Please provide a summary of the work completed so far,
                                including what has been accomplished and what remains to be done. Be concise but informative.
                                """;
                        Message summaryRequest = github.ponyhuang.agentframework.types.message.UserMessage.create(summaryPrompt);
                        List<Message> summaryMessages = new ArrayList<>(convMessages);
                        summaryMessages.add(summaryRequest);

                        ChatResponse summaryResponse = client.chat(ChatCompleteParams.builder()
                                .messages(summaryMessages)
                                .build());

                        if (summaryResponse.getMessage() != null) {
                            convMessages.add(summaryResponse.getMessage());
                            sink.next(summaryResponse.getMessage());
                        }
                        sink.complete();
                        return convMessages;
                    }

                    LOG.debug("Step {}/{}", step, maxSteps);

                    // Build chat request - merge tool executor schemas with builder tools
                    List<Map<String, Object>> toolSchemas = new ArrayList<>();

                    if (toolExecutor != null) {
                        toolSchemas.addAll(toolExecutor.getToolSchemas());
                    }

                    List<Map<String, Object>> builderTools = getTools();
                    if (builderTools != null) {
                        for (Map<String, Object> schema : builderTools) {
                            if (!toolSchemas.contains(schema)) {
                                toolSchemas.add(schema);
                            }
                        }
                    }

                    ChatCompleteParams params = ChatCompleteParams.builder()
                            .messages(convMessages)
                            .tools(toolSchemas)
                            .build();

                    // Call LLM (blocking - wrapped in Mono for async semantics)
                    ChatResponse response = null;
                    Exception lastException = null;

                    try {
                        response = client.chat(params);
                    } catch (Exception e) {
                        LOG.warn("Primary model failed: {}", e.getMessage());
                        lastException = e;

                        // Try fallback model if configured
                        if (fallbackModel != null && !fallbackModel.isEmpty()) {
                            LOG.info("Attempting fallback model: {}", fallbackModel);
                            try {
                                ChatCompleteParams fallbackParams = ChatCompleteParams.builder()
                                        .messages(convMessages)
                                        .tools(toolSchemas)
                                        .model(fallbackModel)
                                        .build();
                                response = client.chat(fallbackParams);
                                lastException = null;
                                LOG.info("Fallback model succeeded");
                            } catch (Exception fallbackError) {
                                LOG.warn("Fallback model also failed: {}", fallbackError.getMessage());
                                lastException = fallbackError;
                            }
                        }
                    }

                    // If both primary and fallback failed, throw exception
                    if (response == null && lastException != null) {
                        String errorMsg = "All models failed. Primary error: " + lastException.getMessage();
                        LOG.error(errorMsg);
                        Message errorMessage = github.ponyhuang.agentframework.types.message.UserMessage.create(errorMsg);
                        List<Message> errorMessages = new ArrayList<>(convMessages);
                        errorMessages.add(errorMessage);

                        // Try one more time with a simple message to get a response
                        try {
                            ChatResponse errorResponse = client.chat(ChatCompleteParams.builder()
                                    .messages(errorMessages)
                                    .build());
                            if (errorResponse.getMessage() != null) {
                                convMessages.add(errorResponse.getMessage());
                                sink.next(errorResponse.getMessage());
                            }
                        } catch (Exception ignored) {
                            // Last resort - just complete
                        }
                        sink.complete();
                        return convMessages;
                    }

                    // Track cost from response
                    if (response.getUsage() != null && !costTracker.isUnlimited()) {
                        double cost = response.getUsage().calculateCostUsd(response.getModel());
                        costTracker.addCost(cost);

                        // Check if budget exceeded
                        if (costTracker.isBudgetExceeded()) {
                            LOG.warn("Budget exceeded: ${}/{}", costTracker.getTotalCostUsd(), costTracker.getMaxBudgetUsd());
                            String budgetExceededMsg = """
                                The maximum budget has been exceeded. Cost: $""" + String.format("%.4f", costTracker.getTotalCostUsd()) + """
                                Budget: $""" + String.format("%.4f", costTracker.getMaxBudgetUsd()) + """
                                Please provide a summary of what has been accomplished so far.
                                """;
                            Message budgetMsg = github.ponyhuang.agentframework.types.message.UserMessage.create(budgetExceededMsg);
                            List<Message> budgetMessages = new ArrayList<>(convMessages);
                            budgetMessages.add(budgetMsg);

                            ChatResponse budgetResponse = client.chat(ChatCompleteParams.builder()
                                    .messages(budgetMessages)
                                    .build());

                            if (budgetResponse.getMessage() != null) {
                                convMessages.add(budgetResponse.getMessage());
                                sink.next(budgetResponse.getMessage());
                            }
                            sink.complete();
                            return convMessages;
                        }
                    }

                    // Add assistant message to conversation
                    convMessages.add(response.getMessage());
                    sink.next(response.getMessage());

                    // Check if we have a function call
                    if (!response.hasFunctionCall()) {
                        // No tool call - continue to next iteration to ask LLM again
                        return convMessages;
                    }

                    List<ToolUseBlock> toolCalls = response.getToolCalls();
                    if (toolCalls == null || toolCalls.isEmpty()) {
                        // No valid tool calls - add a tool result message and continue
                        LOG.error("No tool calls found in the response. Please provide a valid tool call.");
                        String noToolResult = "No tools found in the response. Please provide a valid tool call.";
                        Message toolMessage = ResultMessage.create(null, noToolResult);
                        convMessages.add(toolMessage);
                        return convMessages;
                    }

                    ToolUseBlock toolCall = toolCalls.get(0);
                    String functionName = toolCall.getName();
                    Map<String, Object> functionArgs = toolCall.getInput();
                    String toolCallId = toolCall.getId();

                    LOG.info("Executing tool: {}", functionName);

                    // Check permission mode - PLAN mode blocks tool execution
                    if (permissionMode == PermissionMode.PLAN) {
                        String planModeMsg = "PLAN mode is enabled. Tool execution is disabled. Please provide a plan or analysis without executing tools.";
                        Message planMessage = ResultMessage.create(toolCallId, planModeMsg);
                        convMessages.add(planMessage);
                        return convMessages;
                    }

                    // Check if tool is in disallowed list
                    if (disallowedTools != null && !disallowedTools.isEmpty() && disallowedTools.contains(functionName)) {
                        String deniedMsg = "Tool '" + functionName + "' is explicitly disallowed by agent configuration.";
                        Message deniedMessage = ResultMessage.create(toolCallId, deniedMsg);
                        convMessages.add(deniedMessage);
                        return convMessages;
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

                    // Check for task_done signal
                    String resultStr = toolResult != null ? toolResult.toString() : "";
                    if (TaskDoneTool.isTaskComplete(resultStr)) {
                        LOG.info("Task completed at step {}", step);
                        // Note: Assistant message already sent above via sink.next(response.getMessage())
                        // Just complete the flux without sending another message
                        sink.complete();
                        return convMessages;
                    }

                    // Add tool result message (for non-task_done results)
                    Message toolMessage = ResultMessage.create(toolCallId, resultStr);
                    convMessages.add(toolMessage);

                    // Continue to next iteration
                    return convMessages;
                }
        );
    }

    /**
     * Get max steps configured.
     */
    public int getMaxSteps() {
        return maxSteps;
    }

    /**
     * Get the cost tracker for budget enforcement.
     */
    public CostTracker getCostTracker() {
        return costTracker;
    }

    /**
     * Get the allowed tools set.
     */
    public Set<String> getAllowedTools() {
        return allowedTools;
    }

    /**
     * Get the disallowed tools set.
     */
    public Set<String> getDisallowedTools() {
        return disallowedTools;
    }

    /**
     * Get the fallback model.
     */
    public String getFallbackModel() {
        return fallbackModel;
    }

    /**
     * Get the permission mode.
     */
    public PermissionMode getPermissionMode() {
        return permissionMode;
    }

    /**
     * Create a new LoopAgent builder.
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Builder for LoopAgent.
     */
    public static class Builder extends BaseAgent.Builder<LoopAgent, Builder> {

        private ToolExecutor toolExecutor;
        private int maxSteps = DEFAULT_MAX_STEPS;
        private String workingDirectory = System.getProperty("user.dir");

        // Alignment features
        private Set<String> allowedTools = new HashSet<>();
        private Set<String> disallowedTools = new HashSet<>();
        private double maxBudgetUsd = 0.0;
        private String fallbackModel;
        private PermissionMode permissionMode = PermissionMode.DEFAULT;

        /**
         * Sets the working directory for file system and shell operations.
         *
         * @param workingDirectory the directory path
         * @return this builder
         */
        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

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

        /**
         * Sets the ToolExecutor directly for more control.
         *
         * @param toolExecutor the tool executor
         * @return this builder
         */
        public Builder toolExecutor(ToolExecutor toolExecutor) {
            this.toolExecutor = toolExecutor;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        /**
         * Sets the tools that are auto-approved (always allowed).
         */
        public Builder allowedTools(Set<String> allowedTools) {
            if (allowedTools != null) {
                this.allowedTools = new HashSet<>(allowedTools);
            }
            return this;
        }

        /**
         * Sets the tools that are explicitly disallowed (always denied).
         */
        public Builder disallowedTools(Set<String> disallowedTools) {
            if (disallowedTools != null) {
                this.disallowedTools = new HashSet<>(disallowedTools);
            }
            return this;
        }

        /**
         * Sets the maximum budget in USD for the agent execution.
         */
        public Builder maxBudgetUsd(double maxBudgetUsd) {
            this.maxBudgetUsd = maxBudgetUsd > 0 ? maxBudgetUsd : 0.0;
            return this;
        }

        /**
         * Sets the fallback model to use when the primary model fails.
         */
        public Builder fallbackModel(String fallbackModel) {
            this.fallbackModel = fallbackModel;
            return this;
        }

        /**
         * Sets the permission mode for tool execution.
         */
        public Builder permissionMode(PermissionMode permissionMode) {
            this.permissionMode = permissionMode != null ? permissionMode : PermissionMode.DEFAULT;
            return this;
        }

        @Override
        public LoopAgent build() {
            // Initialize built-in tools with working directory
            TaskTools taskTools = new TaskTools();
            SystemTools systemTools = new SystemTools(workingDirectory);
            FileSystemTools fileSystemTools = new FileSystemTools(workingDirectory);
            TaskDoneTool taskDoneTool = new TaskDoneTool();

            // Register built-in tools to tool executor
            if (this.toolExecutor == null) {
                this.toolExecutor = new ToolExecutor();
            }
            this.toolExecutor
                    .registerAnnotated(taskTools)
                    .registerAnnotated(systemTools)
                    .registerAnnotated(fileSystemTools)
                    .registerAnnotated(taskDoneTool);

            // Add all tool schemas to the tools list
            for (Map<String, Object> schema : this.toolExecutor.getToolSchemas()) {
                if (tools == null) {
                    tools = new ArrayList<>();
                }
                if (!tools.contains(schema)) {
                    tools.add(schema);
                }
            }

            return new LoopAgent(this);
        }

        /**
         * Gets the tool executor (for testing or advanced usage).
         *
         * @return the tool executor
         */
        public ToolExecutor getToolExecutor() {
            return toolExecutor;
        }
    }
}