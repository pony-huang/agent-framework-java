package github.ponyhuang.agentframework.a2a.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import github.ponyhuang.agentframework.a2a.client.A2AEvent;

import java.util.List;
import java.util.Map;

public class Message implements A2AEvent {
    @JsonProperty("role")
    private final Role role;
    @JsonProperty("parts")
    private final List<Part> parts;
    @JsonProperty("messageId")
    private final String messageId;
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;

    @JsonCreator
    public Message(
            @JsonProperty("role") Role role,
            @JsonProperty("parts") List<Part> parts,
            @JsonProperty("messageId") String messageId,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.role = role;
        this.parts = parts;
        this.messageId = messageId;
        this.metadata = metadata;
    }

    public Message(Role role, List<Part> parts) {
        this(role, parts, null, null);
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
