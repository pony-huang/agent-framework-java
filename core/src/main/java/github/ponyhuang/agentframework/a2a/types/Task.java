package github.ponyhuang.agentframework.a2a.types;

import java.util.List;

public class Task {
    private final String id;
    private final String contextId;
    private final TaskStatus status;
    private final List<Artifact> artifacts;
    private final List<Message> history;

    public Task(String id, TaskStatus status) {
        this(id, null, status, null, null);
    }

    public Task(String id, String contextId, TaskStatus status, List<Artifact> artifacts, List<Message> history) {
        this.id = id;
        this.contextId = contextId;
        this.status = status;
        this.artifacts = artifacts;
        this.history = history;
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

        public Task build() {
            return new Task(id, contextId, status, artifacts, history);
        }
    }
}
