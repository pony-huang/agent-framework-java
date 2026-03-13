package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.InMemoryAgentSession;
import github.ponyhuang.agentframework.sessions.SessionOptions;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.tools.builtins.FileSystemTools;
import github.ponyhuang.agentframework.tools.builtins.SystemTools;
import github.ponyhuang.agentframework.tools.builtins.TaskDoneTool;
import github.ponyhuang.agentframework.tools.builtins.TaskTools;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.ResultMessage;
import github.ponyhuang.agentframework.types.message.SystemMessage;
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

    protected LoopAgent(Builder builder) {
        super(builder);
        this.toolExecutor = builder.toolExecutor;
        this.maxSteps = builder.maxSteps;
    }

    @Override
    public AgentSession createSession(SessionOptions options) {
        return new InMemoryAgentSession(this, options);
    }

    @Override
    protected Flux<Message> doRun(List<Message> messages, Map<String, Object> options) {
        Message systemMessage = SystemMessage.create().withText(DEFAULT_SYSTEM_PROMPT);
        List<Message> conversationMessages = new ArrayList<>(messages);
        conversationMessages.add(0, systemMessage);
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
                LOG.info("Task completed at step {}", currentStep);
                String message = TaskDoneTool.extractMessage(resultStr);
                Message completionMessage = AssistantMessage.create(message);
                conversationMessages.add(completionMessage);
                return Flux.fromIterable(conversationMessages);
            }


            // Add tool result message (for non-task_done results)
            Message toolMessage = ResultMessage.create(toolCallId, resultStr);
            conversationMessages.add(toolMessage);
        }

        LOG.warn("Max steps reached: {}", maxSteps);

        // Make a final summary request to the LLM
        String summaryPrompt = """
            The maximum number of steps has been reached. Please provide a summary of the work completed so far,
            including what has been accomplished and what remains to be done. Be concise but informative.
            """;

        // Add a user message asking for summary
        Message summaryRequest = github.ponyhuang.agentframework.types.message.UserMessage.create(summaryPrompt);
        List<Message> summaryMessages = new ArrayList<>(conversationMessages);
        summaryMessages.add(summaryRequest);

        // Call LLM without tools for summary
        ChatResponse summaryResponse = client.chat(ChatCompleteParams.builder()
                .messages(summaryMessages)
                .build());

        List<Message> allMessages = new ArrayList<>(conversationMessages);
        if (summaryResponse.getMessage() != null) {
            allMessages.add(summaryResponse.getMessage());
        }
        return Flux.fromIterable(allMessages);
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
     * Builder for LoopAgent.
     */
    public static class Builder extends BaseAgent.Builder<LoopAgent, Builder> {

        private ToolExecutor toolExecutor;
        private int maxSteps = DEFAULT_MAX_STEPS;
        private String workingDirectory = System.getProperty("user.dir");

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