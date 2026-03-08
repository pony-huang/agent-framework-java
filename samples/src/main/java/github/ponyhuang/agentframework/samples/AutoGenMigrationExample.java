package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;

/**
 * Migration Example: Two-Agent Chat
 * 
 * Equivalent to AutoGen's:
 * user_proxy.initiate_chat(assistant, message="Write a haiku about Java")
 */
public class AutoGenMigrationExample {

    public static void main(String[] args) {
        // 1. Configure LLM (equivalent to llm_config)
        ChatClient client = ClientExample.openAIChatClient();

        // 2. Create Assistant Agent (equivalent to AssistantAgent)
        Agent assistant = AgentBuilder.builder()
                .name("Assistant")
                .instructions("You are a helpful AI assistant.")
                .client(client)
                .build();

        // 3. Initiate Chat (equivalent to user_proxy.initiate_chat)
        // In Agent Framework, we use a Session to represent the conversation context.
        // The "User Proxy" is essentially the code driving the session.
        AgentSession session = assistant.createSession();

        System.out.println("User: Write a haiku about Java.");
        ChatResponse response = session.run(session, UserMessage.create("Write a haiku about Java."));

        System.out.println("Assistant: " + response.getMessage().getTextContent());

        // 4. Multi-turn (optional)
        System.out.println("\nUser: Explain it.");
        response = session.run(session, UserMessage.create("Explain it."));
        System.out.println("Assistant: " + response.getMessage().getTextContent());
    }
}
