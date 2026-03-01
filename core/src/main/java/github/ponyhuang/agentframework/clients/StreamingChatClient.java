package github.ponyhuang.agentframework.clients;

import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * Interface for chat clients that support streaming.
 * Extends ChatClient with streaming-specific methods.
 */
public interface StreamingChatClient extends ChatClient {

    /**
     * Sends a chat completion request and gets a streaming response.
     *
     * @param params the chat completion parameters
     * @return a Flux of chat response updates for streaming
     */
    @Override
    Flux<ChatResponse> chatStream(ChatCompleteParams params);
}
