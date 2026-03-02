package github.ponyhuang.agentframework.a2a.types;

import java.util.List;

public class Artifact {
    private final String artifactId;
    private final List<Part> parts;
    private final String index;

    public Artifact(List<Part> parts) {
        this(null, parts, null);
    }

    public Artifact(String artifactId, List<Part> parts, String index) {
        this.artifactId = artifactId;
        this.parts = parts;
        this.index = index;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public List<Part> getParts() {
        return parts;
    }

    public String getIndex() {
        return index;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String artifactId;
        private List<Part> parts;
        private String index;

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder parts(List<Part> parts) {
            this.parts = parts;
            return this;
        }

        public Builder index(String index) {
            this.index = index;
            return this;
        }

        public Artifact build() {
            return new Artifact(artifactId, parts, index);
        }
    }
}
