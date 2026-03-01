package example.agentframework.traeagent;

import github.ponyhuang.agentframework.agents.BaseAgent;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import example.agentframework.traeagent.config.TraeAgentConfig;
import example.agentframework.traeagent.trajectory.TrajectoryRecorder;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * TraeAgent - An LLM-based software engineering agent.
 * Extends BaseAgent to provide multi-turn ReAct execution loop.
 */
public class TraeAgent extends BaseAgent {

    private static final Logger LOG = LoggerFactory.getLogger(TraeAgent.class);

    private final TraeAgentConfig config;
    private final ToolExecutor toolExecutor;
    private final TrajectoryRecorder trajectoryRecorder;
    private final String systemPrompt;

    private boolean taskCompleted = false;
    private String patchContent = "";

    protected TraeAgent(Builder builder) {
        super(builder);
        this.config = builder.config;
        this.toolExecutor = builder.toolExecutor;
        this.trajectoryRecorder = builder.trajectoryRecorder;
        this.systemPrompt = builder.systemPrompt;
    }

    @Override
    protected AgentSession createSession(Map<String, Object> options) {
        return new TraeAgentSession(this, toolExecutor, trajectoryRecorder, options);
    }

    @Override
    protected ChatResponse doRun(List<Message> messages, Map<String, Object> options) {
        List<Message> conversationMessages = new ArrayList<>(messages);
        int maxSteps = config.getMaxSteps();
        int currentStep = 0;

        while (currentStep < maxSteps) {
            currentStep++;
            LOG.info("Step {}/{}", currentStep, maxSteps);

            // Build chat request
            ChatCompleteParams params = ChatCompleteParams.builder()
                    .messages(conversationMessages)
                    .tools(toolExecutor.getToolSchemas())
                    .build();

            // Record LLM call
            if (trajectoryRecorder != null) {
                trajectoryRecorder.recordLLMCall(params);
            }

            // Call LLM
            ChatResponse response = client.chat(params);

            // Record LLM response
            if (trajectoryRecorder != null) {
                trajectoryRecorder.recordLLMResponse(response);
            }

            // Add assistant message to conversation
            Message assistantMessage = response.getMessage();
            conversationMessages.add(assistantMessage);

            // Check if we have a function call
            if (!response.hasFunctionCall()) {
                // No function call, return the response
                if (trajectoryRecorder != null) {
                    trajectoryRecorder.finish();
                }
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

            // Record tool call
            if (trajectoryRecorder != null) {
                trajectoryRecorder.recordToolCall(functionName, functionArgs);
            }

            // Execute tool
            Object toolResult;
            try {
                toolResult = toolExecutor.execute(functionName, functionArgs != null ? functionArgs : Collections.emptyMap());
            } catch (Exception e) {
                LOG.error("Tool execution failed: {}", e.getMessage());
                toolResult = "Error: " + e.getMessage();
            }

            // Record tool result
            if (trajectoryRecorder != null) {
                trajectoryRecorder.recordToolResult(functionName, toolResult);
            }

            // Check for task completion
            if ("task_done".equals(functionName)) {
                handleTaskDone(functionArgs, toolResult);
            }

            // Check for patch generation
            if ("bash".equals(functionName) && functionArgs != null) {
                String command = (String) functionArgs.get("command");
                if (command != null && command.contains("git diff")) {
                    // Capture patch content
                    if (toolResult instanceof String) {
                        patchContent = (String) toolResult;
                    }
                }
            }

            // Add tool result message
            String resultStr = toolResult != null ? toolResult.toString() : "null";
            Message toolMessage = Message.tool(toolCallId, functionName, resultStr);
            conversationMessages.add(toolMessage);

            // Check if task is completed
            if (taskCompleted) {
                LOG.info("Task completed at step {}", currentStep);
                if (trajectoryRecorder != null) {
                    trajectoryRecorder.finish();
                }
                // Return final response with completion info
                return response;
            }
        }

        LOG.warn("Max steps reached: {}", maxSteps);
        if (trajectoryRecorder != null) {
            trajectoryRecorder.finish();
        }

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
        // For now, delegate to regular run - streaming can be added later
        return Flux.just(doRun(messages, options));
    }

    /**
     * Handle task_done tool call.
     */
    private void handleTaskDone(Map<String, Object> args, Object result) {
        if (args != null) {
            Object done = args.get("done");
            if (done instanceof Boolean && (Boolean) done) {
                taskCompleted = true;
            }
            // Check for must_patch requirement
            Object mustPatch = args.get("must_patch");
            if (mustPatch instanceof Boolean && (Boolean) mustPatch) {
                if (patchContent == null || patchContent.isEmpty()) {
                    LOG.warn("Task requires patch but no changes were made");
                    taskCompleted = false;
                }
            }
        }
        // Also check string result
        if (result instanceof String resultStr) {
            if (resultStr.toLowerCase().contains("\"done\": true") ||
                    resultStr.toLowerCase().contains("\"done\":true")) {
                taskCompleted = true;
            }
        }
    }

    /**
     * Create a new task with context.
     */
    public ChatResponse newTask(String task, Map<String, Object> extraArgs) {
        StringBuilder userMessage = new StringBuilder();

        // Add project path if available
        if (extraArgs != null && extraArgs.containsKey("project_path")) {
            userMessage.append("Project path: ").append(extraArgs.get("project_path")).append("\n\n");
        }

        // Add issue/task description
        userMessage.append("Task: ").append(task);

        // Add additional context
        if (extraArgs != null && extraArgs.containsKey("issue")) {
            userMessage.append("\n\nIssue description: ").append(extraArgs.get("issue"));
        }

        // Create session and run
        AgentSession session = createSession(extraArgs);
        return session.run(userMessage.toString());
    }

    /**
     * Reset the agent for a new task.
     */
    public void reset() {
        taskCompleted = false;
        patchContent = "";
    }

    /**
     * Create a new TraeAgent builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TraeAgent.
     */
    public static class Builder extends BaseAgent.Builder<TraeAgent, Builder> {

        private TraeAgentConfig config;
        private ToolExecutor toolExecutor;
        private TrajectoryRecorder trajectoryRecorder;
        private String systemPrompt = TraeAgentPrompts.DEFAULT_SYSTEM_PROMPT;

        public Builder config(TraeAgentConfig config) {
            this.config = config;
            return this;
        }

        public Builder toolExecutor(ToolExecutor toolExecutor) {
            this.toolExecutor = toolExecutor;
            return this;
        }

        public Builder trajectoryRecorder(TrajectoryRecorder trajectoryRecorder) {
            this.trajectoryRecorder = trajectoryRecorder;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        @Override
        public TraeAgent build() {
            // Use system prompt as instructions
            if (this.instructions == null && systemPrompt != null) {
                this.instructions = systemPrompt;
            }
            return new TraeAgent(this);
        }
    }

    // Getters

    public TraeAgentConfig getConfig() {
        return config;
    }

    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }

    public TrajectoryRecorder getTrajectoryRecorder() {
        return trajectoryRecorder;
    }

    public boolean isTaskCompleted() {
        return taskCompleted;
    }

    public String getPatchContent() {
        return patchContent;
    }
}