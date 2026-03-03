package github.ponyhuang.agentframework.a2a.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import github.ponyhuang.agentframework.a2a.client.A2AEvent;

import java.util.List;

public class Task implements A2AEvent {
    @JsonProperty("id")
    private final String id;
    @JsonProperty("contextId")
    private final String contextId;
    @JsonProperty("status")
    private final TaskStatus status;
    @JsonProperty("artifacts")
    private final List<Artifact> artifacts;
    @JsonProperty("history")
    private final List<Message> history;
    @JsonProperty("messages")
    private final List<Message> messages;

    @JsonCreator
    public Task(
            @JsonProperty("id") String id,
            @JsonProperty("contextId") String contextId,
            @JsonProperty("status") TaskStatus status,
            @JsonProperty("artifacts") List<Artifact> artifacts,
            @JsonProperty("history") List<Message> history,
            @JsonProperty("messages") List<Message> messages) {
        this.id = id;
        this.contextId = contextId;
        this.status = status;
        this.artifacts = artifacts;
        this.history = history;
        this.messages = messages;
    }

    public Task(String id, TaskStatus status) {
        this(id, null, status, null, null, null);
    }

    public String getId() {
        return id;
    }

    public String getContextId() {
        return contextId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public List<Message> getHistory() {
        return history;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public TaskState getState() {
        return status != null ? status.getState() : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String contextId;
        private TaskStatus status;
        private List<Artifact> artifacts;
        private List<Message> history;
        private List<Message> messages;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public Builder history(List<Message> history) {
            this.history = history;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Task build() {
            return new Task(id, contextId, status, artifacts, history, messages);
        }
    }
}
