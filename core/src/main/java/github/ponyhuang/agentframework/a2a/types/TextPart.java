package github.ponyhuang.agentframework.a2a.types;

import java.util.Map;

public class TextPart implements Part {
    private final String text;
    private final Map<String, Object> metadata;

    public TextPart(String text) {
        this(text, null);
    }

    public TextPart(String text, Map<String, Object> metadata) {
        this.text = text;
        this.metadata = metadata;
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
