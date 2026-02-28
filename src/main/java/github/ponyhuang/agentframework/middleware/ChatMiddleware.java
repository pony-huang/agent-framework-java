package github.ponyhuang.agentframework.middleware;

import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;

import java.util.function.Function;

/**
 * Middleware for intercepting chat client execution.
 * Can modify the request/response or terminate early.
 */
public interface ChatMiddleware {

    /**
     * Processes the chat request.
     *
     * @param context the chat context
     * @param next   the next handler in the chain
     * @return the chat response
     */
    ChatResponse process(ChatMiddlewareContext context, Function<ChatMiddlewareContext, ChatResponse> next);

    /**
     * Context for chat middleware.
     */
    class ChatMiddlewareContext {
        private final ChatClient client;
        private final ChatCompleteParams params;
        private ChatResponse response;
        private java.util.Map<String, Object> metadata;

        public ChatMiddlewareContext(ChatClient client, ChatCompleteParams params) {
            this.client = client;
            this.params = params;
            this.metadata = new java.util.HashMap<>();
        }

        public ChatClient getClient() {
            return client;
        }

        public ChatCompleteParams getParams() {
            return params;
        }

        public void setParams(ChatCompleteParams params) {
            // Allow modification
        }

        public ChatResponse getResponse() {
            return response;
        }

        public void setResponse(ChatResponse response) {
            this.response = response;
        }

        public java.util.Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }
    }
}
