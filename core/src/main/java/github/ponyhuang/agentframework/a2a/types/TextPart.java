package github.ponyhuang.agentframework.a2a.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class TextPart implements Part {
    @JsonProperty("text")
    private final String text;
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;

    @JsonCreator
    public TextPart(
            @JsonProperty("text") String text,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.text = text;
        this.metadata = metadata;
    }

    public TextPart(String text) {
        this(text, null);
    }

    @Override
    public String getKind() {
        return "text";
    }

    public String getText() {
        return text;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String text;
        private Map<String, Object> metadata;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public TextPart build() {
            return new TextPart(text, metadata);
        }
    }
}
