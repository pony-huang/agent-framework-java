package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory implementation of AgentSession.
 * Stores messages and state in memory.
 */
public class InMemoryAgentSession implements AgentSession {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryAgentSession.class);

    private final String id;
    private final Agent agent;
    private final List<Message> messages;
    private final Map<String, Object> metadata;

    public InMemoryAgentSession(Agent agent) {
        this.id = UUID.randomUUID().toString();
        this.agent = agent;
        this.messages = new ArrayList<>();
        this.metadata = new HashMap<>();
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
            LOG.debug("Message added to session, role: {}", message.getRole());
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
    public Flux<ChatResponse> runStream(Message message) {
        addMessage(message);
        LOG.debug("Running stream for session: {}", id);
        Flux<ChatResponse> responseFlux = getAgent().runStream(getMessages());
        return responseFlux.doOnNext(response -> {
            if (response.getMessage() != null) {
                addMessage(response.getMessage());
            }
        });
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
}
