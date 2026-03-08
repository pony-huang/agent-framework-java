package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.HookExecutor;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.events.SessionStartContext;
import github.ponyhuang.agentframework.hooks.events.StopContext;
import github.ponyhuang.agentframework.hooks.events.UserPromptSubmitContext;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.ContextProvider;
import github.ponyhuang.agentframework.sessions.SessionOptions;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.SystemMessage;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class for Agent implementations.
 * Provides common functionality for building agents.
 */
public abstract class BaseAgent implements Agent {

    protected static final Logger LOG = LoggerFactory.getLogger(BaseAgent.class);

    protected String name;
    protected String instructions;
    protected ChatClient client;
    protected List<Map<String, Object>> tools;
    protected List<ContextProvider> contextProviders;
    protected Map<String, Object> defaultOptions;

    protected BaseAgent() {
        this.tools = new ArrayList<>();
        this.contextProviders = new ArrayList<>();
        this.defaultOptions = new HashMap<>();
    }

    protected BaseAgent(Builder<?, ?> builder) {
        this.name = builder.name;
        this.instructions = builder.instructions;
        this.client = builder.client;
        this.tools = builder.tools != null ? new ArrayList<>(builder.tools) : new ArrayList<>();
        this.contextProviders = builder.contextProviders != null ? new ArrayList<>(builder.contextProviders) : new ArrayList<>();
        this.defaultOptions = builder.defaultOptions != null ? new HashMap<>(builder.defaultOptions) : new HashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getInstructions() {
        return instructions;
    }

    @Override
    public ChatClient getClient() {
        return client;
    }

    @Override
    public List<Map<String, Object>> getTools() {
        return new ArrayList<>(tools);
    }

    @Override
    public List<ContextProvider> getContextProviders() {
        return new ArrayList<>(contextProviders);
    }

    @Override
    public Agent addTool(Map<String, Object> tool) {
        if (tool != null) {
            tools.add(tool);
        }
        return this;
    }

    @Override
    public Agent addContextProvider(ContextProvider provider) {
        if (provider != null) {
            contextProviders.add(provider);
        }
        return this;
    }

    @Override
    public Agent removeTool(String toolName) {
        if (toolName != null) {
            tools.removeIf(tool -> toolName.equals(tool.get("name")));
        }
        return this;
    }

    /**
     * Creates a new session for this agent.
     *
     * @return a new session
     */
    @Override
    public AgentSession createSession() {
        return createSession(SessionOptions.builder().build());
    }

    /**
     * Creates a new session with options.
     *
     * @param options session options
     * @return a new session
     */
    public abstract AgentSession createSession(SessionOptions options);

    /**
     * Merges default options with runtime options.
     *
     * @param runtimeOptions runtime options (may be null)
     * @return merged options
     */
    protected Map<String, Object> mergeOptions(Map<String, Object> runtimeOptions) {
        Map<String, Object> merged = new HashMap<>(defaultOptions);
        if (runtimeOptions != null) {
            merged.putAll(runtimeOptions);
        }
        return merged;
    }

    /**
     * Prepares messages for the chat request.
     * Adds system message with instructions if present.
     *
     * @param messages input messages
     * @return prepared messages
     */
    protected List<Message> prepareMessages(List<Message> messages) {
        List<Message> prepared = new ArrayList<>();

        // Add instructions as system message if present
        if (instructions != null && !instructions.isEmpty()) {
            prepared.add(SystemMessage.create(instructions));
        }

        // Add user messages
        if (messages != null) {
            prepared.addAll(messages);
        }

        return prepared;
    }

    /**
     * Abstract method to implement the run logic.
     *
     * @param messages input messages
     * @param options options
     * @return flux of messages
     */
    protected abstract Flux<Message> doRun(List<Message> messages, Map<String, Object> options);

    public ChatResponse run(List<Message> messages, Map<String, Object> options) {
        LOG.info("Agent '{}' run started, message count: {}", name, messages != null ? messages.size() : 0);
        Map<String, Object> mergedOptions = mergeOptions(options);

        HookExecutor hookExecutor = getHookExecutor();

        if (hookExecutor != null) {
            SessionStartContext sessionStartContext = new SessionStartContext();
            sessionStartContext.setSessionId(UUID.randomUUID().toString());
            sessionStartContext.setSource("startup");
            sessionStartContext.setModel(client != null ? client.getModel() : "unknown");
            sessionStartContext.setCwd(System.getProperty("user.dir"));
            sessionStartContext.setPermissionMode("default");
            hookExecutor.executeSessionStart(sessionStartContext);

            if (messages != null && !messages.isEmpty()) {
                UserPromptSubmitContext promptContext = new UserPromptSubmitContext();
                promptContext.setSessionId(sessionStartContext.getSessionId());
                Message lastMessage = messages.get(messages.size() - 1);
                promptContext.setPrompt(lastMessage.getTextContent());
                HookResult promptResult = hookExecutor.executeUserPromptSubmit(promptContext);
                if (!promptResult.isAllow()) {
                    LOG.info("UserPromptSubmit hook blocked execution: {}", promptResult.getReason());
                    return ChatResponse.builder()
                            .messages(List.of())
                            .build();
                }
            }
        }

        try {
            List<Message> preparedMessages = prepareMessages(messages);
            Flux<Message> messageFlux = doRun(preparedMessages, mergedOptions);
            List<Message> collectedMessages = messageFlux.collectList().block();

            ChatResponse response = ChatResponse.builder()
                    .messages(collectedMessages)
                    .build();

            LOG.info("Agent '{}' run completed", name);

            fireStopHook(hookExecutor, response);

            return response;
        } catch (Exception e) {
            LOG.error("Agent '{}' run failed: {}", name, e.getMessage());
            throw e;
        }
    }

    private void fireStopHook(HookExecutor hookExecutor, ChatResponse response) {
        if (hookExecutor != null) {
            StopContext stopContext = new StopContext();
            stopContext.setStopHookActive(false);
            if (response != null && response.getMessage() != null) {
                StringBuilder sb = new StringBuilder();
                github.ponyhuang.agentframework.types.message.Message msg = response.getMessage();
                if (msg.getBlocks() != null) {
                    msg.getBlocks().forEach(b -> {
                        if (b instanceof github.ponyhuang.agentframework.types.block.TextBlock) {
                            sb.append(((github.ponyhuang.agentframework.types.block.TextBlock) b).getText());
                        }
                    });
                }
                stopContext.setLastAssistantMessage(sb.toString());
            }
            HookResult result = hookExecutor.executeStop(stopContext);
            if (!result.isShouldContinue()) {
                LOG.info("Stop hook prevented completion: {}", result.getStopReason());
            }
        }
    }

    @Override
    public Flux<Message> runStream(List<Message> messages, Map<String, Object> options) {
        LOG.info("Agent '{}' stream started, message count: {}", name, messages != null ? messages.size() : 0);
        Map<String, Object> mergedOptions = mergeOptions(options);
        return doRun(prepareMessages(messages), mergedOptions)
                .doOnComplete(() -> LOG.info("Agent '{}' stream completed", name))
                .doOnError(e -> LOG.error("Agent '{}' stream failed: {}", name, e.getMessage()));
    }

    /**
     * Builder base class for agents.
     *
     * @param <T> the agent type
     * @param <B> the builder type
     */
    protected abstract static class Builder<T extends BaseAgent, B extends Builder<T, B>> {
        protected String name = "assistant";
        protected String instructions;
        protected ChatClient client;
        protected List<Map<String, Object>> tools;
        protected List<ContextProvider> contextProviders;
        protected Map<String, Object> defaultOptions;

        @SuppressWarnings("unchecked")
        public B name(String name) {
            this.name = name;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B instructions(String instructions) {
            this.instructions = instructions;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B client(ChatClient client) {
            this.client = client;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B tools(List<Map<String, Object>> tools) {
            this.tools = tools;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B contextProviders(List<ContextProvider> contextProviders) {
            this.contextProviders = contextProviders;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B addTool(Map<String, Object> tool) {
            if (this.tools == null) {
                this.tools = new ArrayList<>();
            }
            this.tools.add(tool);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B addContextProvider(ContextProvider provider) {
            if (this.contextProviders == null) {
                this.contextProviders = new ArrayList<>();
            }
            this.contextProviders.add(provider);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B defaultOptions(Map<String, Object> defaultOptions) {
            this.defaultOptions = defaultOptions;
            return (B) this;
        }

        protected abstract T build();
    }
}
