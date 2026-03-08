package example.agentframework.codeagent;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.ContextProvider;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import example.agentframework.codeagent.trajectory.TrajectoryRecorder;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Session implementation for LoopAgent.
 */
public class CodeAgentSession implements AgentSession {

    private final CodeAgent agent;
    private final ToolExecutor toolExecutor;
    private final TrajectoryRecorder trajectoryRecorder;
    private final Map<String, Object> options;
    private final String id;
    private final List<Message> messages = new ArrayList<>();
    private final Map<String, Object> metadata = new HashMap<>();

    public CodeAgentSession(CodeAgent agent, ToolExecutor toolExecutor,
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
            this.messages.add(message);
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
        if (limit <= 0 || messages.isEmpty()) {
            return new ArrayList<>();
        }
        int start = Math.max(0, messages.size() - limit);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    @Override
    public void clearHistory() {
        this.messages.clear();
    }

    @Override
    public ChatResponse run(Message message) {
        if (message != null) {
            this.messages.add(message);
        }
        
        Map<String, Object> runOptions = new HashMap<>(options);
        
        // Pre-run hooks
        for (ContextProvider provider : agent.getContextProviders()) {
            List<Message> updatedMessages = provider.beforeRun(agent, this, new ArrayList<>(this.messages), runOptions);
            this.messages.clear();
            this.messages.addAll(updatedMessages);
        }
        
        ChatResponse response = agent.run(new ArrayList<>(this.messages));
        
        // Post-run hooks
        for (ContextProvider provider : agent.getContextProviders()) {
            provider.afterRun(agent, this, new ArrayList<>(this.messages), response, runOptions);
        }
        
        if (response.getMessage() != null) {
            this.messages.add(response.getMessage());
        }
        return response;
    }

    @Override
    public Flux<Message> runStream(Message message) {
        if (message != null) {
            this.messages.add(message);
        }
        return agent.runStream(new ArrayList<>(this.messages))
                .doOnNext(msg -> {
                    if (msg != null) {
                        this.messages.add(msg);
                    }
                });
    }

    @Override
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    @Override
    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
}
