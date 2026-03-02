package github.ponyhuang.agentframework.a2a.types;

import java.util.Map;

public class FilePart implements Part {
    private final FileReference file;
    private final Map<String, Object> metadata;

    public FilePart(FileReference file) {
        this(file, null);
    }

    public FilePart(FileReference file, Map<String, Object> metadata) {
        this.file = file;
        this.metadata = metadata;
    }

    @Override
    public String getKind() {
        return "file";
    }

    public FileReference getFile() {
        return file;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FileReference file;
        private Map<String, Object> metadata;

        public Builder file(FileReference file) {
            this.file = file;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public FilePart build() {
            return new FilePart(file, metadata);
        }
    }
}
