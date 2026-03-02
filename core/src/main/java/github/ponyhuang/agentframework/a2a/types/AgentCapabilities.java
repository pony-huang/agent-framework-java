package github.ponyhuang.agentframework.a2a.types;

import java.util.List;
import java.util.Map;

public class AgentCapabilities {
    private final Boolean streaming;
    private final Boolean pushNotifications;
    private final Boolean stateTransitionHistory;
    private final List<String> supportedMethods;
    private final Map<String, Object> additionalProperties;

    public AgentCapabilities() {
        this(null, null, null, null, null);
    }

    public AgentCapabilities(Boolean streaming, Boolean pushNotifications, Boolean stateTransitionHistory,
                           List<String> supportedMethods, Map<String, Object> additionalProperties) {
        this.streaming = streaming;
        this.pushNotifications = pushNotifications;
        this.stateTransitionHistory = stateTransitionHistory;
        this.supportedMethods = supportedMethods;
        this.additionalProperties = additionalProperties;
    }

    public Boolean getStreaming() {
        return streaming;
    }

    public Boolean getPushNotifications() {
        return pushNotifications;
    }

    public Boolean getStateTransitionHistory() {
        return stateTransitionHistory;
    }

    public List<String> getSupportedMethods() {
        return supportedMethods;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean streaming;
        private Boolean pushNotifications;
        private Boolean stateTransitionHistory;
        private List<String> supportedMethods;
        private Map<String, Object> additionalProperties;

        public Builder streaming(Boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder pushNotifications(Boolean pushNotifications) {
            this.pushNotifications = pushNotifications;
            return this;
        }

        public Builder stateTransitionHistory(Boolean stateTransitionHistory) {
            this.stateTransitionHistory = stateTransitionHistory;
            return this;
        }

        public Builder supportedMethods(List<String> supportedMethods) {
            this.supportedMethods = supportedMethods;
            return this;
        }

        public Builder additionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public AgentCapabilities build() {
            return new AgentCapabilities(streaming, pushNotifications, stateTransitionHistory,
                    supportedMethods, additionalProperties);
        }
    }
}
