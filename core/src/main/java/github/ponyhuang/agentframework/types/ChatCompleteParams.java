package github.ponyhuang.agentframework.types;

import github.ponyhuang.agentframework.types.message.Message;
import java.util.List;
import java.util.Map;

/**
 * Parameters for a chat completion request.
 */
public class ChatCompleteParams {

    private final List<Message> messages;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final Double topP;
    private final List<Map<String, Object>> tools;
    private final String system;
    private final String stop;
    private final Integer n;
    private final Double presencePenalty;
    private final Double frequencyPenalty;
    private final Map<String, Object> extraProperties;

    private ChatCompleteParams(Builder builder) {
        this.messages = builder.messages;
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.topP = builder.topP;
        this.tools = builder.tools;
        this.system = builder.system;
        this.stop = builder.stop;
        this.n = builder.n;
        this.presencePenalty = builder.presencePenalty;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.extraProperties = builder.extraProperties;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public String getModel() {
        return model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public List<Map<String, Object>> getTools() {
        return tools;
    }

    public String getSystem() {
        return system;
    }

    public String getStop() {
        return stop;
    }

    public Integer getN() {
        return n;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public Map<String, Object> getExtraProperties() {
        return extraProperties;
    }

    /**
     * Gets the effective system message.
     * If there's a system message in the messages list, returns that.
     * Otherwise returns the system field.
     *
     * @return the system message content, or null
     */
    public String getEffectiveSystem() {
        if (system != null) {
            return system;
        }
        if (messages != null) {
            return messages.stream()
                    .filter(m -> "system".equalsIgnoreCase(m.getRoleAsString()))
                    .map(Message::getTextContent)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ChatCompleteParams.
     */
    public static class Builder {
        private List<Message> messages;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private List<Map<String, Object>> tools;
        private String system;
        private String stop;
        private Integer n;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Object> extraProperties;

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder tools(List<Map<String, Object>> tools) {
            this.tools = tools;
            return this;
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder stop(String stop) {
            this.stop = stop;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder extraProperties(Map<String, Object> extraProperties) {
            this.extraProperties = extraProperties;
            return this;
        }

        public ChatCompleteParams build() {
            return new ChatCompleteParams(this);
        }
    }
}
