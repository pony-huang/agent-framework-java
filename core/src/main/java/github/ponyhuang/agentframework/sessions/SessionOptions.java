package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionOptions {

    private List<Message> initialMessages = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private SessionExecutor customExecutor;

    public List<Message> getInitialMessages() {
        return initialMessages;
    }

    public void setInitialMessages(List<Message> initialMessages) {
        this.initialMessages = initialMessages != null ? initialMessages : new ArrayList<>();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public SessionExecutor getCustomExecutor() {
        return customExecutor;
    }

    public void setCustomExecutor(SessionExecutor customExecutor) {
        this.customExecutor = customExecutor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SessionOptions options = new SessionOptions();

        public Builder initialMessages(List<Message> messages) {
            options.setInitialMessages(messages);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            options.setMetadata(metadata);
            return this;
        }

        public Builder customExecutor(SessionExecutor executor) {
            options.setCustomExecutor(executor);
            return this;
        }

        public SessionOptions build() {
            return options;
        }
    }
}
