package github.ponyhuang.agentframework.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Represents a response from a chat completion.
 */
public class ChatResponse {

    private final String id;
    private final long created;
    private final String model;
    private final List<Choice> choices;
    private final Usage usage;
    private final String finishReason;
    private final Map<String, Object> extraProperties;

    private ChatResponse(Builder builder) {
        this.id = builder.id;
        this.created = builder.created;
        this.model = builder.model;
        this.choices = builder.choices;
        this.usage = builder.usage;
        this.finishReason = builder.finishReason;
        this.extraProperties = builder.extraProperties;
    }

    public String getId() {
        return id;
    }



    public long getCreated() {
        return created;
    }

    public String getModel() {
        return model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public Map<String, Object> getExtraProperties() {
        return extraProperties;
    }

    /**
     * Gets the first choice's message.
     *
     * @return the message, or null if no choices
     */
    public Message getMessage() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).getMessage();
    }

    /**
     * Checks if the response contains a function call.
     *
     * @return true if any choice has a function call
     */
    public boolean hasFunctionCall() {
        if (choices == null) {
            return false;
        }
        return choices.stream()
                .anyMatch(c -> c.getMessage() != null && c.getMessage().getFunctionCall() != null);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Represents a choice in the response.
     */
    public static class Choice {
        private final int index;
        private final Message message;
        private final String finishReason;

        public Choice(int index, Message message, String finishReason) {
            this.index = index;
            this.message = message;
            this.finishReason = finishReason;
        }

        public int getIndex() {
            return index;
        }

        public Message getMessage() {
            return message;
        }

        public String getFinishReason() {
            return finishReason;
        }
    }

    /**
     * Represents token usage information.
     */
    public static class Usage {
        private final int promptTokens;
        private final int completionTokens;
        private final int totalTokens;

        public Usage(int promptTokens, int completionTokens, int totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public int getPromptTokens() {
            return promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }
    }

    /**
     * Builder for ChatResponse.
     */
    public static class Builder {
        private String id;
        private long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
        private String finishReason;
        private Map<String, Object> extraProperties;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder created(long created) {
            this.created = created;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder choices(List<Choice> choices) {
            this.choices = choices;
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }

        public Builder extraProperties(Map<String, Object> extraProperties) {
            this.extraProperties = extraProperties;
            return this;
        }

        public ChatResponse build() {
            return new ChatResponse(this);
        }
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException ignored) {
        }
        return super.toString();
    }
}
