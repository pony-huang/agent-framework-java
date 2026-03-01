package github.ponyhuang.agentframework.types;

import java.util.List;

/**
 * Represents an embedding vector.
 */
public class Embedding {

    private final String objectType;
    private final List<Double> embedding;
    private final int index;

    private Embedding(Builder builder) {
        this.objectType = builder.objectType;
        this.embedding = builder.embedding;
        this.index = builder.index;
    }

    public String getObjectType() {
        return objectType;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public int getIndex() {
        return index;
    }

    /**
     * Gets the dimension of the embedding vector.
     *
     * @return the dimension, or 0 if embedding is empty
     */
    public int getDimension() {
        return embedding != null ? embedding.size() : 0;
    }

    public Builder builder() {
        return new Builder();
    }

    /**
     * Builder for Embedding.
     */
    public static class Builder {
        private String objectType = "embedding";
        private List<Double> embedding;
        private int index;

        public Builder objectType(String objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder embedding(List<Double> embedding) {
            this.embedding = embedding;
            return this;
        }

        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Embedding build() {
            return new Embedding(this);
        }
    }
}
