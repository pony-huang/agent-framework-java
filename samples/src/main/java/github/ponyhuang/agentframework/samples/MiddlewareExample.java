package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.middleware.AgentMiddleware;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import github.ponyhuang.agentframework.types.Role;

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
            System.out.println("[Middleware] Before Agent Run: " + context.getMessages().get(context.getMessages().size() - 1).getText());
            
            // Continue to next middleware or agent
            ChatResponse response = next.apply(context);
            
            System.out.println("[Middleware] After Agent Run: " + response.getMessage().getText());
            return response;
        }
    }
    // </logging_middleware>

    // <safety_middleware>
    static class ContentSafetyMiddleware implements AgentMiddleware {
        @Override
        public ChatResponse process(AgentMiddlewareContext context, Function<AgentMiddlewareContext, ChatResponse> next) {
            String lastUserMessage = context.getMessages().stream()
                    .filter(m -> m.getRole() == Role.USER)
                    .reduce((first, second) -> second)
                    .map(Message::getText)
                    .orElse("");

            if (lastUserMessage.toLowerCase().contains("unsafe")) {
                System.out.println("[Middleware] Blocked unsafe content!");
                // Return a blocked response immediately, bypassing the agent
                return ChatResponse.builder()
                        .choices(java.util.List.of(new ChatResponse.Choice(0, Message.assistant("I cannot process unsafe content."), "stop")))
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
        ChatResponse response1 = agent.run(List.of(Message.user("Hello, how are you?")));
        System.out.println("Final Response: " + response1.getMessage().getText() + "\n");

        System.out.println("--- Test 2: Unsafe Content ---");
        ChatResponse response2 = agent.run(List.of(Message.user("This is unsafe content.")));
        System.out.println("Final Response: " + response2.getMessage().getText() + "\n");
    }
}
