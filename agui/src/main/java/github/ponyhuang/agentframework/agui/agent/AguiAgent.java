//package github.ponyhuang.agentframework.agui.agent;
//
//import com.agui.core.agent.Agent;
//import com.agui.core.agent.AgentSubscriber;
//import com.agui.core.agent.AgentSubscriberParams;
//import com.agui.core.agent.RunAgentParameters;
//import com.agui.core.event.*;
//import com.agui.core.message.BaseMessage;
//import github.ponyhuang.agentframework.agents.BaseAgent;
//import github.ponyhuang.agentframework.agui.MessageConverter;
//import github.ponyhuang.agentframework.clients.ChatClient;
//import github.ponyhuang.agentframework.sessions.AgentSession;
//import github.ponyhuang.agentframework.tools.FunctionTool;
//import github.ponyhuang.agentframework.tools.ToolExecutor;
//import github.ponyhuang.agentframework.types.ChatCompleteParams;
//import github.ponyhuang.agentframework.types.ChatResponse;
//import github.ponyhuang.agentframework.types.block.ToolResultBlock;
//import github.ponyhuang.agentframework.types.block.ToolUseBlock;
//import github.ponyhuang.agentframework.types.message.Message;
//import github.ponyhuang.agentframework.types.message.UserMessage;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import reactor.core.publisher.Flux;
//
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.function.Function;
//
//public class AguiAgent extends BaseAgent implements Agent {
//
//    private static final Logger LOG = LoggerFactory.getLogger(AguiAgent.class);
//
//    private final List<FunctionTool> agentTools = new CopyOnWriteArrayList<>();
//    private final List<AgentSubscriber> subscribers = new CopyOnWriteArrayList<>();
//    private final int maxSteps;
//    private final Function<String, Boolean> terminationHandler;
//
//    protected AguiAgent(Builder builder) {
//        super(builder);
//        this.maxSteps = builder.maxSteps;
//        this.terminationHandler = builder.terminationHandler;
//        if (builder.agentTools != null) {
//            this.agentTools.addAll(builder.agentTools);
//        }
//    }
//
//    @Override
//    public CompletableFuture<Void> runAgent(RunAgentParameters parameters, AgentSubscriber subscriber) {
//        return CompletableFuture.runAsync(() -> {
//            try {
//                AgentSubscriberParams params = new AgentSubscriberParams(
//                        parameters.getMessages(),
//                        parameters.getState(),
//                        this,
//                        null
//                );
//
//                if (subscriber != null) {
//                    subscriber.onRunInitialized(params);
//                }
//
//                publishEvent(subscriber, () -> {
//                    RunStartedEvent event = new RunStartedEvent();
//                    event.setThreadId(parameters.getThreadId());
//                    event.setRunId(parameters.getRunId());
//                    return event;
//                });
//
//                List<Message> messages = MessageConverter.toFrameworkMessages(
//                        parameters.getMessages() != null ? parameters.getMessages() : List.of());
//
//                List<FunctionTool> effectiveTools = new ArrayList<>(this.agentTools);
//                if (parameters.getTools() != null) {
//                    for (com.agui.core.tool.Tool tool : parameters.getTools()) {
//                        FunctionTool existing = effectiveTools.stream()
//                                .filter(t -> t.getName().equals(tool.name()))
//                                .findFirst()
//                                .orElse(null);
//                        if (existing == null) {
//                            FunctionTool ft = FunctionTool.builder()
//                                    .name(tool.name())
//                                    .description(tool.description())
//                                    .build();
//                            effectiveTools.add(ft);
//                        }
//                    }
//                }
//
//                ToolExecutor toolExecutor = null;
//                if (!effectiveTools.isEmpty()) {
//                    toolExecutor = new ToolExecutor();
//                    for (FunctionTool tool : effectiveTools) {
//                        toolExecutor.register(tool);
//                    }
//                }
//
//                ChatClient effectiveClient = this.client;
//                if (parameters.getForwardedProps() instanceof Map<?, ?> props) {
//                    if (props.get("client") instanceof ChatClient providedClient) {
//                        effectiveClient = providedClient;
//                    }
//                }
//
//                int step = 0;
//                while (step < maxSteps) {
//                    step++;
//
//                    final int currentStep = step;
//                    publishEvent(subscriber, () -> {
//                        StepStartedEvent event = new StepStartedEvent();
//                        event.setStepName("step-" + currentStep);
//                        return event;
//                    });
//
//                    ChatCompleteParams chatParams = ChatCompleteParams.builder()
//                            .messages(messages)
//                            .tools(toolExecutor != null ? toolExecutor.getToolSchemas() : List.of())
//                            .build();
//
//                    ChatResponse response = effectiveClient.chat(chatParams);
//
//                    Message assistantMessage = response.getMessage();
//                    messages.add(assistantMessage);
//
//                    String messageId = UUID.randomUUID().toString();
//                    publishEvent(subscriber, () -> {
//                        TextMessageStartEvent event = new TextMessageStartEvent();
//                        event.setMessageId(messageId);
//                        return event;
//                    });
//
//                    String textContent = assistantMessage.getTextContent();
//                    if (textContent != null && !textContent.isEmpty()) {
//                        publishEvent(subscriber, () -> {
//                            TextMessageContentEvent event = new TextMessageContentEvent();
//                            event.setMessageId(messageId);
//                            event.setDelta(textContent);
//                            return event;
//                        });
//                    }
//
//                    publishEvent(subscriber, () -> {
//                        TextMessageEndEvent event = new TextMessageEndEvent();
//                        event.setMessageId(messageId);
//                        return event;
//                    });
//
//                    if (!response.hasFunctionCall()) {
//                        break;
//                    }
//
//                    if (assistantMessage.getBlocks().getLast() instanceof ToolUseBlock toolCall) {
//                        String functionName = toolCall.getName();
//                        Map<String, Object> functionArgs = toolCall.getInput();
//                        String toolCallId = toolCall.getId();
//
//                        publishEvent(subscriber, () -> {
//                            ToolCallStartEvent event = new ToolCallStartEvent();
//                            event.setToolCallId(toolCallId);
//                            event.setToolCallName(functionName);
//                            event.setParentMessageId(messageId);
//                            return event;
//                        });
//
//                        if (functionArgs != null && !functionArgs.isEmpty()) {
//                            publishEvent(subscriber, () -> {
//                                ToolCallArgsEvent event = new ToolCallArgsEvent();
//                                event.setToolCallId(toolCallId);
//                                event.setDelta(functionArgs.toString());
//                                return event;
//                            });
//                        }
//
//                        publishEvent(subscriber, () -> {
//                            ToolCallEndEvent event = new ToolCallEndEvent();
//                            event.setToolCallId(toolCallId);
//                            return event;
//                        });
//
//                        Object toolResult;
//                        try {
//                            if (toolExecutor != null) {
//                                toolResult = toolExecutor.execute(functionName,
//                                        functionArgs != null ? functionArgs : Collections.emptyMap());
//                            } else {
//                                toolResult = "Error: No tool executor configured";
//                            }
//                        } catch (Exception e) {
//                            LOG.error("Tool execution failed: {}", e.getMessage());
//                            toolResult = "Error: " + e.getMessage();
//                        }
//
//                        final String resultStr = toolResult != null ? toolResult.toString() : "null";
//                        publishEvent(subscriber, () -> {
//                            ToolCallResultEvent event = new ToolCallResultEvent();
//                            event.setToolCallId(toolCallId);
//                            event.setContent(resultStr);
//                            event.setMessageId(messageId);
//                            return event;
//                        });
//
//                        ToolResultBlock toolResultBlock = new ToolResultBlock(toolCallId, resultStr);
//                        messages.add(UserMessage.create(List.of(toolResultBlock)));
//
//                        if (terminationHandler != null && terminationHandler.apply(functionName)) {
//                            LOG.info("Termination handler signaled stop at step {}", currentStep);
//                            break;
//                        }
//
//                        publishEvent(subscriber, () -> {
//                            StepFinishedEvent event = new StepFinishedEvent();
//                            event.setStepName("step-" + currentStep);
//                            return event;
//                        });
//                    }
//
//                }
//
//                publishEvent(subscriber, () -> {
//                    RunFinishedEvent event = new RunFinishedEvent();
//                    return event;
//                });
//
//                if (subscriber != null) {
//                    subscriber.onRunFinalized(params);
//                }
//
//            } catch (Exception e) {
//                LOG.error("Agent run failed: {}", e.getMessage(), e);
//
//                publishEvent(subscriber, () -> {
//                    RunErrorEvent event = new RunErrorEvent();
//                    event.setError(e.getMessage());
//                    return event;
//                });
//
//                if (subscriber != null) {
//                    AgentSubscriberParams params = new AgentSubscriberParams(
//                            List.of(),
//                            null,
//                            this,
//                            null
//                    );
//                    subscriber.onRunFailed(params, e);
//                }
//            }
//        });
//    }
//
//    @Override
//    protected AgentSession createSession(Map<String, Object> options) {
//        ToolExecutor executor = new ToolExecutor();
//        for (FunctionTool tool : agentTools) {
//            executor.register(tool);
//        }
//        return new AguiAgentSession(this, this.client, executor, options);
//    }
//
//    @Override
//    protected Flux<Message> doRun(List<Message> messages, Map<String, Object> options) {
//        return Flux.empty();
//    }
//
//    @Override
//    public List<BaseMessage> getMessages() {
//        return List.of();
//    }
//
//    public void addSubscriber(AgentSubscriber subscriber) {
//        if (subscriber != null) {
//            subscribers.add(subscriber);
//        }
//    }
//
//    public void removeSubscriber(AgentSubscriber subscriber) {
//        subscribers.remove(subscriber);
//    }
//
//    public void addTool(FunctionTool tool) {
//        if (tool != null) {
//            agentTools.add(tool);
//        }
//    }
//
//    private void publishEvent(AgentSubscriber subscriber, java.util.function.Supplier<BaseEvent> eventFactory) {
//        if (subscriber == null && subscribers.isEmpty()) {
//            return;
//        }
//
//        BaseEvent event = eventFactory.get();
//        if (event == null) {
//            return;
//        }
//
//        if (subscriber != null) {
//            notifySubscriber(subscriber, event);
//        }
//
//        for (AgentSubscriber s : subscribers) {
//            notifySubscriber(s, event);
//        }
//    }
//
//    private void notifySubscriber(AgentSubscriber subscriber, BaseEvent event) {
//        try {
//            subscriber.onEvent(event);
//            switch (event.getType()) {
//                case RUN_STARTED -> subscriber.onRunStartedEvent((RunStartedEvent) event);
//                case RUN_FINISHED -> subscriber.onRunFinishedEvent((RunFinishedEvent) event);
//                case RUN_ERROR -> subscriber.onRunErrorEvent((RunErrorEvent) event);
//                case STEP_STARTED -> subscriber.onStepStartedEvent((StepStartedEvent) event);
//                case STEP_FINISHED -> subscriber.onStepFinishedEvent((StepFinishedEvent) event);
//                case TEXT_MESSAGE_START -> subscriber.onTextMessageStartEvent((TextMessageStartEvent) event);
//                case TEXT_MESSAGE_CONTENT -> subscriber.onTextMessageContentEvent((TextMessageContentEvent) event);
//                case TEXT_MESSAGE_END -> subscriber.onTextMessageEndEvent((TextMessageEndEvent) event);
//                case TOOL_CALL_START -> subscriber.onToolCallStartEvent((ToolCallStartEvent) event);
//                case TOOL_CALL_END -> subscriber.onToolCallEndEvent((ToolCallEndEvent) event);
//                case TOOL_CALL_ARGS -> subscriber.onToolCallArgsEvent((ToolCallArgsEvent) event);
//                case TOOL_CALL_RESULT -> subscriber.onToolCallResultEvent((ToolCallResultEvent) event);
//                case STATE_SNAPSHOT -> subscriber.onStateSnapshotEvent((StateSnapshotEvent) event);
//                case STATE_DELTA -> subscriber.onStateDeltaEvent((StateDeltaEvent) event);
//                case MESSAGES_SNAPSHOT -> subscriber.onMessagesSnapshotEvent((MessagesSnapshotEvent) event);
//                case RAW -> subscriber.onRawEvent((RawEvent) event);
//                case CUSTOM -> subscriber.onCustomEvent((CustomEvent) event);
//                default -> {
//                }
//            }
//        } catch (Exception e) {
//            LOG.error("Error notifying subscriber: {}", e.getMessage(), e);
//        }
//    }
//
//    public static Builder builder() {
//        return new Builder();
//    }
//
//    public static class Builder extends BaseAgent.Builder<AguiAgent, Builder> {
//
//        private List<FunctionTool> agentTools;
//        private int maxSteps = 10;
//        private Function<String, Boolean> terminationHandler;
//
//        public Builder addTools(List<FunctionTool> tools) {
//            this.agentTools = tools;
//            return this;
//        }
//
//        public Builder addTool(FunctionTool tool) {
//            if (this.agentTools == null) {
//                this.agentTools = new ArrayList<>();
//            }
//            this.agentTools.add(tool);
//            return this;
//        }
//
//        public Builder maxSteps(int maxSteps) {
//            this.maxSteps = maxSteps;
//            return this;
//        }
//
//        public Builder terminationHandler(Function<String, Boolean> terminationHandler) {
//            this.terminationHandler = terminationHandler;
//            return this;
//        }
//
//        @Override
//        public AguiAgent build() {
//            return new AguiAgent(this);
//        }
//    }
//}
