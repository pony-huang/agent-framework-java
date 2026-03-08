package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Simple example showing basic agent usage.
 */
public class SimpleAgentExample {

    public static void main(String[] args) {
        // Create a chat client (using OpenAI as example)
        ChatClient client = ClientExample.openAIChatClient();

        // Build an agent
        Agent agent = AgentBuilder.builder()
                .name("assistant")
                .instructions("You are a helpful assistant.")
                .client(client)
                .build();

        // Run the agent
        List<Message> messages = agent.runStream(List.of(UserMessage.create("Hello, how are you?"))).collectList().block();
        ChatResponse response = ChatResponse.builder()
                .messages(messages)
                .build();

        System.out.println("Response: " + response.getMessage().getTextContent());
    }
}
