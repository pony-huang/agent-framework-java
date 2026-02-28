package example.agentframework.traeagent;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.ContextProvider;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import example.agentframework.traeagent.trajectory.TrajectoryRecorder;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Session implementation for TraeAgent.
 */
public class TraeAgentSession implements AgentSession {

    private final TraeAgent agent;
    private final ToolExecutor toolExecutor;
    private final TrajectoryRecorder trajectoryRecorder;
    private final Map<String, Object> options;
    private final String id;
    private final List<Message> messages = new ArrayList<>();
    private final Map<String, Object> metadata = new HashMap<>();

    public TraeAgentSession(TraeAgent agent, ToolExecutor toolExecutor,
                           TrajectoryRecorder trajectoryRecorder, Map<String, Object> options) {
        this.agent = agent;
        this.toolExecutor = toolExecutor;
        this.trajectoryRecorder = trajectoryRecorder;
        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
        this.id = UUID.randomUUID().toString();
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
        }
    }

    @Override
    public void addMessages(List<Message> messages) {
        if (messages != null) {
            this.messages.addAll(messages);
        }
    }

    @Override
    public List<Message> getHistory(int limit) {
        if (limit <= 0 || messages.size() <= limit) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(messages.size() - limit, messages.size()));
    }

    @Override
    public void clearHistory() {
        messages.clear();
    }

    @Override
    public ChatResponse run(Message message) {
        addMessage(message);

        // Get context providers before run
        List<ContextProvider> providers = agent.getContextProviders();
        List<Message> currentMessages = getMessages();
        Map<String, Object> runOptions = new HashMap<>(this.options);

        // Pre-run hooks
        for (ContextProvider provider : providers) {
            currentMessages = provider.beforeRun(agent, this, currentMessages, runOptions);
        }

        ChatResponse response = agent.run(currentMessages);

        // Post-run hooks
        for (ContextProvider provider : providers) {
            provider.afterRun(agent, this, currentMessages, response, runOptions);
        }

        if (response.getMessage() != null) {
            addMessage(response.getMessage());
        }

        return response;
    }

    @Override
    public Flux<ChatResponse> runStream(Message message) {
        addMessage(message);
        return agent.runStream(getMessages(), options);
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

    /**
     * Get session options.
     */
    public Map<String, Object> getOptions() {
        return new HashMap<>(options);
    }
}