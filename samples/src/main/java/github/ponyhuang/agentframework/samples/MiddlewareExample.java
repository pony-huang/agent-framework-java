package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.middleware.AgentMiddleware;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

/**
 * Example demonstrating the use of Middleware to intercept and modify agent execution.
 */
public class MiddlewareExample {

    // <logging_middleware>
    static class LoggingMiddleware implements AgentMiddleware {
        @Override
        public ChatResponse process(AgentMiddlewareContext context, Function<AgentMiddlewareContext, ChatResponse> next) {
            System.out.println("[Middleware] Before Agent Run: " + context.getMessages().get(context.getMessages().size() - 1).getTextContent());
            
            // Continue to next middleware or agent
            ChatResponse response = next.apply(context);
            
            System.out.println("[Middleware] After Agent Run: " + response.getMessage().getTextContent());
            return response;
        }
    }
    // </logging_middleware>

    // <safety_middleware>
    static class ContentSafetyMiddleware implements AgentMiddleware {
        @Override
        public ChatResponse process(AgentMiddlewareContext context, Function<AgentMiddlewareContext, ChatResponse> next) {
            String lastUserMessage = context.getMessages().stream()
                    .filter(m -> "user".equalsIgnoreCase(m.getRoleAsString()))
                    .reduce((first, second) -> second)
                    .map(Message::getTextContent)
                    .orElse("");

            if (lastUserMessage.toLowerCase().contains("unsafe")) {
                System.out.println("[Middleware] Blocked unsafe content!");
                // Return a blocked response immediately, bypassing the agent
                return ChatResponse.builder()
                        .messages(java.util.List.of(AssistantMessage.create("I cannot process unsafe content.")))
                        .build();
            }

            return next.apply(context);
        }
    }
    // </safety_middleware>

    public static void main(String[] args) {
        ChatClient client = ClientExample.openAIChatClient();

        Agent agent = AgentBuilder.builder()
                .name("MiddlewareAgent")
                .instructions("You are a helpful assistant.")
                .client(client)
                .middleware(new LoggingMiddleware())
                .middleware(new ContentSafetyMiddleware())
                .build();

        System.out.println("--- Test 1: Safe Content ---");
        List<Message> messages1 = agent.runStream(List.of(UserMessage.create("Hello, how are you?"))).collectList().block();
        ChatResponse response1 = ChatResponse.builder().messages(messages1).build();
        System.out.println("Final Response: " + response1.getMessage().getTextContent() + "\n");

        System.out.println("--- Test 2: Unsafe Content ---");
        List<Message> messages2 = agent.runStream(List.of(UserMessage.create("This is unsafe content."))).collectList().block();
        ChatResponse response2 = ChatResponse.builder().messages(messages2).build();
        System.out.println("Final Response: " + response2.getMessage().getTextContent() + "\n");
    }
}
