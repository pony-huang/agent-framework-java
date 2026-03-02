package github.ponyhuang.agentframework.a2a.types;

public class FileWithId implements FileReference {
    private final String fileId;

    public FileWithId(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public String getKind() {
        return "id";
    }

    public String getFileId() {
        return fileId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String fileId;

        public Builder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        public FileWithId build() {
            return new FileWithId(fileId);
        }
    }
}
