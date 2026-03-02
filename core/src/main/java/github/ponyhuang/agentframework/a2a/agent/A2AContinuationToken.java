package github.ponyhuang.agentframework.a2a.agent;

import java.util.Map;

public class A2AContinuationToken {
    private final String taskId;
    private final String contextId;

    public A2AContinuationToken(String taskId, String contextId) {
        this.taskId = taskId;
        this.contextId = contextId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getContextId() {
        return contextId;
    }

    public Map<String, String> toMap() {
        return Map.of(
            "task_id", taskId,
            "context_id", contextId != null ? contextId : ""
        );
    }

    public static A2AContinuationToken fromMap(Map<String, String> map) {
        return new A2AContinuationToken(
            map.get("task_id"),
            map.get("context_id")
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String taskId;
        private String contextId;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public A2AContinuationToken build() {
            return new A2AContinuationToken(taskId, contextId);
        }
    }
}
