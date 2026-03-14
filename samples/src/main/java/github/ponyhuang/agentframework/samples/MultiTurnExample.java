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
 * Example demonstrating multi-turn conversation using AgentSession.
 */
public class MultiTurnExample {

    public static void main(String[] args) {
        // Create a chat client (using OpenAI as example)
        // Ensure you have set the environment variables: MY_OPENAI_API_KEY, MY_OPENAI_BASE_URL, MY_OPENAI_MODEL
        // Or for Anthropic: MY_ANTHROPIC_AUTH_TOKEN, MY_ANTHROPIC_BASE_URL, MY_ANTHROPIC_MODEL
        ChatClient client = ClientExample.openAIChatClient();

        // Build an agent
        Agent agent = AgentBuilder.builder()
                .name("ConversationAgent")
                .instructions("You are a friendly assistant. Keep your answers brief.")
                .client(client)
                .build();

        // Create a session to maintain conversation history
        Session session = new DefaultSession("session-1");

        System.out.println("Starting multi-turn conversation...\n");

        // First turn
        String input1 = "My name is Alice and I love hiking.";
        System.out.println("User: " + input1);
        Message message = agent.runStream(session, List.of(UserMessage.create(input1))).blockLast();
        if (message != null) {
            System.out.println("Agent: " + message.getTextContent() + "\n");
        } else {
            System.out.println("Agent: [No response text]\n");
        }

        // Second turn - the agent should remember the user's name and hobby
        String input2 = "What do you remember about me?";
        System.out.println("User: " + input2);
        Message message2 = agent.runStream(session, List.of(UserMessage.create(input2))).blockLast();
        if (message2 != null) {
            System.out.println("Agent: " + message2.getTextContent());
        } else {
            System.out.println("Agent: [No response text]\n");
        }
    }
}
