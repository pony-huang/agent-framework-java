package github.ponyhuang.agentframework.clients;

import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import reactor.core.publisher.Flux;

/**
 * Abstract base class for chat clients.
 * Provides common functionality for implementations.
 */
public abstract class DefaultChatClient implements ChatClient {

    protected String model;

    protected DefaultChatClient() {
    }

    protected DefaultChatClient(String model) {
        this.model = model;
    }

    @Override
    public String getModel() {
        return model;
    }

    /**
     * Sets the model name.
     *
     * @param model the model name
     * @return this builder for chaining
     */
    public DefaultChatClient model(String model) {
        this.model = model;
        return this;
    }

    /**
     * Creates default chat params with the given messages.
     *
     * @param messages the messages
     * @return the chat params
     */
    protected ChatCompleteParams createParams(java.util.List<Message> messages) {
        ChatCompleteParams.Builder builder = ChatCompleteParams.builder()
                .messages(messages);
        if (model != null) {
            builder.model(model);
        }
        return builder.build();
    }

    /**
     * Creates default chat params with model override.
     *
     * @param messages the messages
     * @param model   the model (overrides default)
     * @return the chat params
     */
    protected ChatCompleteParams createParams(java.util.List<Message> messages, String model) {
        return ChatCompleteParams.builder()
                .messages(messages)
                .model(model != null ? model : this.model)
                .build();
    }

    @Override
    public Flux<ChatResponse> chatStream(ChatCompleteParams params) {
        throw new UnsupportedOperationException("Streaming not supported by this implementation");
    }
}
