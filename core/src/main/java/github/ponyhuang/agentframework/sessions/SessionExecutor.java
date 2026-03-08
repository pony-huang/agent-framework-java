package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import reactor.core.publisher.Flux;

public interface SessionExecutor extends AutoCloseable {

    ChatResponse run(ConversationSession session, Message message);

    Flux<Message> runStream(ConversationSession session, Message message);

    @Override
    default void close() {
    }
}
