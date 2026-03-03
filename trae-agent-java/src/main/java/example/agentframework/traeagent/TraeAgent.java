package example.agentframework.traeagent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.*;
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
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final List<AgentSubscriber> subscribers = new CopyOnWriteArrayList<>();

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
        // Publish RUN_STARTED event
        publishEvent(() -> {
            RunStartedEvent event = new RunStartedEvent();
            event.setRawEvent(Map.of("task", messages.isEmpty() ? "" : messages.get(0).getText()));
            return event;
        });

        List<Message> conversationMessages = new ArrayList<>(messages);
        int maxSteps = config.getMaxSteps();
        int currentStep = 0;

        while (currentStep < maxSteps) {
            currentStep++;
            LOG.info("Step {}/{}", currentStep, maxSteps);

            // Publish STEP_STARTED event
            final int step = currentStep;
            publishEvent(() -> {
                StepStartedEvent event = new StepStartedEvent();
                event.setStepName("step-" + step);
                return event;
            });

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

            // Publish TEXT_MESSAGE_START event
            final String messageId = UUID.randomUUID().toString();
            publishEvent(() -> {
                TextMessageStartEvent event = new TextMessageStartEvent();
                event.setMessageId(messageId);
                event.setRole("assistant");
                return event;
            });

            // Publish TEXT_MESSAGE_CONTENT event
            String textContent = assistantMessage.getText();
            if (textContent != null && !textContent.isEmpty()) {
                publishEvent(() -> {
                    TextMessageContentEvent event = new TextMessageContentEvent();
                    event.setMessageId(messageId);
                    event.setDelta(textContent);
                    return event;
                });
            }

            // Publish TEXT_MESSAGE_END event
            publishEvent(() -> {
                TextMessageEndEvent event = new TextMessageEndEvent();
                event.setMessageId(messageId);
                return event;
            });

            // Check if we have a function call
            if (!response.hasFunctionCall()) {
                // No function call, return the response
                if (trajectoryRecorder != null) {
                    trajectoryRecorder.finish();
                }

                // Publish STEP_FINISHED event
                publishEvent(() -> {
                    StepFinishedEvent event = new StepFinishedEvent();
                    event.setStepName("step-" + step);
                    return event;
                });

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

            // Publish TOOL_CALL_START event
            publishEvent(() -> {
                ToolCallStartEvent event = new ToolCallStartEvent();
                event.setToolCallId(toolCallId);
                event.setToolCallName(functionName);
                event.setParentMessageId(messageId);
                return event;
            });

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

            // Publish TOOL_CALL_RESULT event
            publishEvent(() -> {
                ToolCallResultEvent event = new ToolCallResultEvent();
                event.setToolCallId(toolCallId);
                event.setContent(resultStr);
                event.setMessageId(messageId);
                return event;
            });

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
     * Remove a subscriber from receiving agent events.
     *
     * @param subscriber the subscriber to remove
     */
    public void removeSubscriber(AgentSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Publish an event to all subscribers.
     *
     * @param eventFactory a factory that creates the event
     */
    private void publishEvent(java.util.function.Supplier<BaseEvent> eventFactory) {
        if (subscribers.isEmpty()) {
            return;
        }
        BaseEvent event = eventFactory.get();
        if (event != null) {
            for (AgentSubscriber subscriber : subscribers) {
                try {
                    subscriber.onEvent(event);
                    // Also call specific event handler based on type
                    switch (event.getType()) {
                        case RUN_STARTED -> subscriber.onRunStartedEvent((RunStartedEvent) event);
                        case RUN_FINISHED -> subscriber.onRunFinishedEvent((RunFinishedEvent) event);
                        case RUN_ERROR -> subscriber.onRunErrorEvent((RunErrorEvent) event);
                        case STEP_STARTED -> subscriber.onStepStartedEvent((StepStartedEvent) event);
                        case STEP_FINISHED -> subscriber.onStepFinishedEvent((StepFinishedEvent) event);
                        case TEXT_MESSAGE_START -> subscriber.onTextMessageStartEvent((TextMessageStartEvent) event);
                        case TEXT_MESSAGE_CONTENT -> subscriber.onTextMessageContentEvent((TextMessageContentEvent) event);
                        case TEXT_MESSAGE_END -> subscriber.onTextMessageEndEvent((TextMessageEndEvent) event);
                        case TOOL_CALL_START -> subscriber.onToolCallStartEvent((ToolCallStartEvent) event);
                        case TOOL_CALL_END -> subscriber.onToolCallEndEvent((ToolCallEndEvent) event);
                        case TOOL_CALL_ARGS -> subscriber.onToolCallArgsEvent((ToolCallArgsEvent) event);
                        case TOOL_CALL_RESULT -> subscriber.onToolCallResultEvent((ToolCallResultEvent) event);
                        case STATE_SNAPSHOT -> subscriber.onStateSnapshotEvent((StateSnapshotEvent) event);
                        case STATE_DELTA -> subscriber.onStateDeltaEvent((StateDeltaEvent) event);
                        case MESSAGES_SNAPSHOT -> subscriber.onMessagesSnapshotEvent((MessagesSnapshotEvent) event);
                        case RAW -> subscriber.onRawEvent((RawEvent) event);
                        case CUSTOM -> subscriber.onCustomEvent((CustomEvent) event);
                        default -> { }
                    }
                } catch (Exception e) {
                    LOG.error("Error notifying subscriber: {}", e.getMessage(), e);
                }
            }
        }
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