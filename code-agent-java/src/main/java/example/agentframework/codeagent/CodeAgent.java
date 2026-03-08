package example.agentframework.codeagent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.RunStartedEvent;
import com.agui.core.event.StepStartedEvent;
import example.agentframework.codeagent.config.AgentConfig;
import example.agentframework.codeagent.trajectory.TrajectoryRecorder;
import github.ponyhuang.agentframework.agents.BaseAgent;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.ResultMessage;
import github.ponyhuang.agentframework.types.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * LoopAgent - An LLM-based software engineering agent.
 * Extends BaseAgent to provide multi-turn ReAct execution loop.
 */
public class CodeAgent extends BaseAgent {

    private static final Logger LOG = LoggerFactory.getLogger(CodeAgent.class);

    private final AgentConfig config;
    private final ToolExecutor toolExecutor;
    private final TrajectoryRecorder trajectoryRecorder;
    private final String systemPrompt;
    private final List<AgentSubscriber> subscribers = new CopyOnWriteArrayList<>();

    private boolean taskCompleted = false;
    private String patchContent = "";

    protected CodeAgent(Builder builder) {
        super(builder);
        this.config = builder.config;
        this.toolExecutor = builder.toolExecutor;
        this.trajectoryRecorder = builder.trajectoryRecorder;
        this.systemPrompt = builder.systemPrompt;
    }

    @Override
    protected AgentSession createSession(Map<String, Object> options) {
        return new CodeAgentSession(this, toolExecutor, trajectoryRecorder, options);
    }

    @Override
    protected Flux<Message> doRun(List<Message> messages, Map<String, Object> options) {


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

                return Flux.fromIterable(conversationMessages);
            }

            // Handle function calls - ReAct loop
            // Extract function call from ToolUseBlock
            List<Map<String, Object>> functionCalls = extractFunctionCalls(assistantMessage);
            if (functionCalls.isEmpty()) {
                break;
            }

            for (Map<String, Object> functionCall : functionCalls) {
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

                final String resultStr = toolResult != null ? toolResult.toString() : "null";


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
                Message toolMessage = ResultMessage.create(toolCallId, functionName, resultStr);
                conversationMessages.add(toolMessage);

                // Check if task is completed
                if (taskCompleted) {
                    LOG.info("Task completed at step {}", currentStep);
                    if (trajectoryRecorder != null) {
                        trajectoryRecorder.finish();
                    }
                    // Return final response with completion info
                    return Flux.fromIterable(conversationMessages);
                }
            }
        }

        LOG.warn("Max steps reached: {}", maxSteps);
        if (trajectoryRecorder != null) {
            trajectoryRecorder.finish();
        }

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

    private List<Map<String, Object>> extractFunctionCalls(Message message) {
        List<Map<String, Object>> calls = new ArrayList<>();

        // Try old-style functionCall map first
        if (message instanceof AssistantMessage) {
            AssistantMessage assistantMsg = (AssistantMessage) message;
            Map<String, Object> functionCall = assistantMsg.getFunctionCall();
            if (functionCall != null) {
                calls.add(functionCall);
                return calls;
            }
        }

        // Try new-style ToolUseBlock
        if (message.getBlocks() != null) {
            for (Block block : message.getBlocks()) {
                if (block instanceof ToolUseBlock) {
                    ToolUseBlock toolUse = (ToolUseBlock) block;
                    Map<String, Object> call = new HashMap<>();
                    call.put("id", toolUse.getId());
                    call.put("name", toolUse.getName());
                    call.put("arguments", toolUse.getInput());
                    calls.add(call);
                }
            }
        }

        return calls;
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
        return session.run(UserMessage.create(userMessage.toString()));
    }

    /**
     * Reset the agent for a new task.
     */
    public void reset() {
        taskCompleted = false;
        patchContent = "";
    }

    /**
     * Add a subscriber to receive agent events.
     *
     * @param subscriber the subscriber to add
     */
    public void addSubscriber(AgentSubscriber subscriber) {
        if (subscriber != null) {
            subscribers.add(subscriber);
        }
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
    public static class Builder extends BaseAgent.Builder<CodeAgent, Builder> {

        private AgentConfig config;
        private ToolExecutor toolExecutor;
        private TrajectoryRecorder trajectoryRecorder;
        private String systemPrompt = CodeAgentPrompts.DEFAULT_SYSTEM_PROMPT;

        public Builder config(AgentConfig config) {
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
        public CodeAgent build() {
            // Use system prompt as instructions
            if (this.instructions == null && systemPrompt != null) {
                this.instructions = systemPrompt;
            }
            return new CodeAgent(this);
        }
    }

    // Getters

    public AgentConfig getConfig() {
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
