package github.ponyhuang.agentframework.clients;

import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for chat completion clients.
 * Implementations handle communication with LLM providers.
 */
public interface ChatClient {

    /**
     * Sends a chat completion request and gets a synchronous response.
     *
     * @param params the chat completion parameters
     * @return the chat response
     */
    ChatResponse chat(ChatCompleteParams params);

    /**
     * Sends a chat completion request and gets a streaming response.
     *
     * @param params the chat completion parameters
     * @return a Flux of chat response updates for streaming
     */
    Flux<ChatResponse> chatStream(ChatCompleteParams params);

    /**
     * Sends a chat completion request and gets an asynchronous response.
     *
     * @param params the chat completion parameters
     * @return a Mono containing the chat response
     */
    default Mono<ChatResponse> chatAsync(ChatCompleteParams params) {
        return Mono.fromCallable(() -> chat(params));
    }

    /**
     * Gets the model name used by this client.
     *
     * @return the model name
     */
    default String getModel() {
        return null;
    }
}
