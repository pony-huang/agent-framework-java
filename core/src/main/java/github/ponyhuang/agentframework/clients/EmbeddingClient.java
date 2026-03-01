package github.ponyhuang.agentframework.clients;

import github.ponyhuang.agentframework.types.Embedding;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Interface for embedding clients.
 * Implementations handle communication with LLM providers for embedding generation.
 */
public interface EmbeddingClient {

    /**
     * Generates embeddings for the given texts.
     *
     * @param texts the texts to embed
     * @return list of embeddings
     */
    List<Embedding> embed(List<String> texts);

    /**
     * Generates embeddings for the given texts asynchronously.
     *
     * @param texts the texts to embed
     * @return a Mono containing the list of embeddings
     */
    default Mono<List<Embedding>> embedAsync(List<String> texts) {
        return Mono.fromCallable(() -> embed(texts));
    }

    /**
     * Gets the model name used by this client.
     *
     * @return the model name
     */
    default String getModel() {
        return null;
    }

    /**
     * Gets the embedding dimension for this model.
     *
     * @return the dimension, or -1 if unknown
     */
    default int getDimension() {
        return -1;
    }
}
