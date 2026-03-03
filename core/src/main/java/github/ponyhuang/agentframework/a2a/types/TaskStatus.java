package github.ponyhuang.agentframework.a2a.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TaskStatus {
    @JsonProperty("state")
    private final TaskState state;
    @JsonProperty("message")
    private final Message message;
    @JsonProperty("artifacts")
    private final List<Artifact> artifacts;

    @JsonCreator
    public TaskStatus(
            @JsonProperty("state") TaskState state,
            @JsonProperty("message") Message message,
            @JsonProperty("artifacts") List<Artifact> artifacts) {
        this.state = state;
        this.message = message;
        this.artifacts = artifacts;
    }

    public TaskStatus(TaskState state) {
        this(state, null, null);
    }

    public TaskState getState() {
        return state;
    }

    public Message getMessage() {
        return message;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private TaskState state;
        private Message message;
        private List<Artifact> artifacts;

        public Builder state(TaskState state) {
            this.state = state;
            return this;
        }

        public Builder message(Message message) {
            this.message = message;
            return this;
        }

        public Builder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public TaskStatus build() {
            return new TaskStatus(state, message, artifacts);
        }
    }
}
