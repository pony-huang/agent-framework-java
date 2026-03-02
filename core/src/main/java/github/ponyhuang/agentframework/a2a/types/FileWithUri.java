package github.ponyhuang.agentframework.a2a.types;

public class FileWithUri implements FileReference {
    private final String uri;
    private final String mimeType;

    public FileWithUri(String uri) {
        this(uri, null);
    }

    public FileWithUri(String uri, String mimeType) {
        this.uri = uri;
        this.mimeType = mimeType;
    }

    @Override
    public String getKind() {
        return "uri";
    }

    public String getUri() {
        return uri;
    }

    public String getMimeType() {
        return mimeType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uri;
        private String mimeType;

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public FileWithUri build() {
            return new FileWithUri(uri, mimeType);
        }
    }
}
