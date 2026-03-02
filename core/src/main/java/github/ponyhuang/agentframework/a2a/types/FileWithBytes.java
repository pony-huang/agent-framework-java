package github.ponyhuang.agentframework.a2a.types;

public class FileWithBytes implements FileReference {
    private final String bytes;
    private final String mimeType;

    public FileWithBytes(String bytes) {
        this(bytes, null);
    }

    public FileWithBytes(String bytes, String mimeType) {
        this.bytes = bytes;
        this.mimeType = mimeType;
    }

    @Override
    public String getKind() {
        return "bytes";
    }

    public String getBytes() {
        return bytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public byte[] getDecodedBytes() {
        return java.util.Base64.getDecoder().decode(bytes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String bytes;
        private String mimeType;

        public Builder bytes(String bytes) {
            this.bytes = bytes;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public FileWithBytes build() {
            return new FileWithBytes(bytes, mimeType);
        }
    }
}
