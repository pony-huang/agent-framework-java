package github.ponyhuang.agentframework.a2a.types;

import java.util.List;
import java.util.Map;

public class Message {
    private final Role role;
    private final List<Part> parts;
    private final String messageId;
    private final Map<String, Object> metadata;

    public Message(Role role, List<Part> parts) {
        this(role, parts, null, null);
    }

    public Message(Role role, List<Part> parts, String messageId, Map<String, Object> metadata) {
        this.role = role;
        this.parts = parts;
        this.messageId = messageId;
        this.metadata = metadata;
    }

    public Role getRole() {
        return role;
    }

    public List<Part> getParts() {
        return parts;
    }

    public String getMessageId() {
        return messageId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Role role;
        private List<Part> parts;
        private String messageId;
        private Map<String, Object> metadata;

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder parts(List<Part> parts) {
            this.parts = parts;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Message build() {
            return new Message(role, parts, messageId, metadata);
        }
    }
}
