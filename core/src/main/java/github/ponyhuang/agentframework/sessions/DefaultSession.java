package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.message.Message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation of Session.
 */
public class DefaultSession implements Session {

    private final String id;
    private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    private volatile boolean active = false;
    private long timeoutMs = 30 * 60 * 1000; // 30 minutes default
    private int maxMessages = 100;
    private volatile long lastActiveTime = System.currentTimeMillis();

    public DefaultSession(String id) {
        this.id = id;
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
            trimMessages();
            updateLastActiveTime();
        }
    }

    @Override
    public void addMessages(List<Message> messages) {
        if (messages != null) {
            this.messages.addAll(messages);
            trimMessages();
            updateLastActiveTime();
        }
    }

    @Override
    public List<Message> getHistory(int limit) {
        if (limit <= 0) {
            return new ArrayList<>(messages);
        }
        int size = messages.size();
        if (limit >= size) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(size - limit, size));
    }

    @Override
    public void clearHistory() {
        messages.clear();
    }

    @Override
    public void start() {
        this.active = true;
        this.lastActiveTime = System.currentTimeMillis();
    }

    @Override
    public void end() {
        this.active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    @Override
    public void setMetadata(String key, Object value) {
        if (key != null) {
            metadata.put(key, value);
        }
    }

    @Override
    public Object getMetadata(String key) {
        return key != null ? metadata.get(key) : null;
    }

    @Override
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public int getMaxMessages() {
        return maxMessages;
    }

    @Override
    public long getLastActiveTime() {
        return lastActiveTime;
    }

    @Override
    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    private void trimMessages() {
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    public boolean isExpired() {
        if (!active) {
            return true;
        }
        long elapsed = System.currentTimeMillis() - lastActiveTime;
        return elapsed > timeoutMs;
    }
}
