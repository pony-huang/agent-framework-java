package github.ponyhuang.agentframework.a2a.types;

import java.util.Map;

public class DataPart implements Part {
    private final Map<String, Object> data;
    private final Map<String, Object> metadata;

    public DataPart(Map<String, Object> data) {
        this(data, null);
    }

    public DataPart(Map<String, Object> data, Map<String, Object> metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    @Override
    public String getKind() {
        return "data";
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, Object> data;
        private Map<String, Object> metadata;

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public DataPart build() {
            return new DataPart(data, metadata);
        }
    }
}
