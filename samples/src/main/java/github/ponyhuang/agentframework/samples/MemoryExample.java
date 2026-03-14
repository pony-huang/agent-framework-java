package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.sessions.DefaultSession;
import github.ponyhuang.agentframework.sessions.Session;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;

import java.util.List;

/**
 * Example demonstrating memory and context providers.
 */
public class MemoryExample {


    public static void main(String[] args) {
        // Create a chat client (using OpenAI as example)
        // Ensure you have set the environment variables: MY_OPENAI_API_KEY, MY_OPENAI_BASE_URL, MY_OPENAI_MODEL
        ChatClient client = ClientExample.openAIChatClient();

        Agent agent = AgentBuilder.builder()
                .name("MemoryAgent")
                .instructions("You are a friendly assistant.")
                .client(client)
                .build();

        Session session = new DefaultSession("session-1");

        System.out.println("Starting memory example...\n");

        // 1. The provider doesn't know the user yet
        Message message1 = agent.runStream(session, List.of(UserMessage.create("Hello! What's the square root of 9?"))).blockLast();
        System.out.println("Agent: " + message1.getTextContent());

        // 2. Now provide the name
        Message message2 = agent.runStream(session, List.of(UserMessage.create("My name is Alice"))).blockLast();
        System.out.println("Agent: " + message2.getTextContent());

        // 3. Subsequent calls should be personalized
        Message message3 = agent.runStream(session, List.of(UserMessage.create("What is 2 + 2?"))).blockLast();
        System.out.println("Agent: " + message3.getTextContent());

        // Check metadata
        System.out.println("Session Metadata: " + session.getMetadata("user_name"));
    }

}
