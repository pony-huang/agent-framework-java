package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.BaseAgent;
import github.ponyhuang.agentframework.hooks.HookEventBus;
import github.ponyhuang.agentframework.hooks.events.SessionEndContext;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryAgentSession implements AgentSession {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryAgentSession.class);

    private final String id;
    private final Agent agent;
    private final List<Message> messages;
    private final Map<String, Object> metadata;
    private volatile boolean closed = false;

    public InMemoryAgentSession(Agent agent) {
        this(agent, SessionOptions.builder().build());
    }

    public InMemoryAgentSession(Agent agent, SessionOptions options) {
        this.id = UUID.randomUUID().toString();
        this.agent = agent;
        this.messages = new ArrayList<>(options.getInitialMessages());
        this.metadata = new HashMap<>(options.getMetadata());
        LOG.debug("Created InMemoryAgentSession for agent: {}", agent.getName());
    }

    @Override
    public Agent getAgent() {
        return agent;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    @Override
    public void addMessage(Message message) {
        if (message != null) {
            messages.add(message);
            LOG.debug("Message added to session, role: {}", message.getRoleAsString());
        }
    }

    @Override
    public void addMessages(List<Message> messages) {
        if (messages != null) {
            this.messages.addAll(messages);
            LOG.debug("{} messages added to session", messages.size());
        }
    }

    @Override
    public List<Message> getHistory(int limit) {
        if (limit <= 0) {
            return new ArrayList<>(messages);
        }
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(messages.subList(fromIndex, size));
    }

    @Override
    public void clearHistory() {
        messages.clear();
        LOG.debug("Session history cleared");
    }

    @Override
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    @Override
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    @Override
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    @Override
    public ChatResponse run(ConversationSession session, Message message) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        addMessage(message);
        LOG.debug("Running for session: {}", id);
        List<Message> currentMessages = getMessages();
        Flux<Message> messageFlux = getAgent().runStream(currentMessages);
        List<Message> collectedMessages = messageFlux.collectList().block();
        return ChatResponse.builder()
                .messages(collectedMessages)
                .build();
    }

    @Override
    public Flux<Message> runStream(ConversationSession session, Message message) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        addMessage(message);
        LOG.debug("Running stream for session: {}", id);
        Flux<Message> messageFlux = getAgent().runStream(getMessages());
        return messageFlux.doOnNext(msg -> {
            if (msg != null) {
                addMessage(msg);
            }
        });
    }

    @Override
    public void close() {
        if (closed) return;
        this.closed = true;
        LOG.debug("Session {} closed", id);

        HookEventBus hookEventBus = null;
        if (agent instanceof BaseAgent) {
            hookEventBus = ((BaseAgent) agent).getHookEventBus();
        }
        if (hookEventBus != null) {
            SessionEndContext context = new SessionEndContext();
            context.setSessionId(id);
            context.setReason("user_closed");
            context.setCwd(System.getProperty("user.dir"));
            context.setPermissionMode("default");
            hookEventBus.executeSessionEnd(context);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
