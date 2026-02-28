package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.providers.AnthropicChatClient;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Example showing asynchronous streaming using OpenAIChatClient#chatStream.
 */
public class AsyncChatStreamExample {

    public static void main(String[] args) throws InterruptedException {
        AnthropicChatClient client = ClientExample.anthropicChatClient();

        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(Message.user("Explain machine learning in one short paragraph.")))
                .build();

        Flux<ChatResponse> stream = client.chatStream(params);

        CountDownLatch done = new CountDownLatch(1);
        StringBuilder buffer = new StringBuilder();

        stream.subscribe(
                chunk -> {
                    if (chunk == null || chunk.getMessage() == null) {
                        return;
                    }
                    String text = chunk.getMessage().getText();
                    if (text != null && !text.isBlank()) {
                        System.out.print(text);
                        buffer.append(text);
                    }
                },
                error -> {
                    System.err.println("Stream error: " + error.getMessage());
                    done.countDown();
                },
                () -> {
                    System.out.println();
                    System.out.println("Final: " + buffer);
                    done.countDown();
                }
        );

        if (!done.await(60, TimeUnit.SECONDS)) {
            System.err.println("Stream timed out.");
        }
    }
}
