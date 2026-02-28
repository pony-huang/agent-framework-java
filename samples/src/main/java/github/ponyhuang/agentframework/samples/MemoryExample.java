package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.ContextProvider;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import github.ponyhuang.agentframework.types.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating memory and context providers.
 */
public class MemoryExample {

    // <context_provider>
    static class UserMemoryProvider implements ContextProvider {
        
        @Override
        public List<Message> beforeRun(Object agent, AgentSession session, List<Message> messages, Map<String, Object> options) {
            String userName = (String) session.getMetadata("user_name");
            List<Message> newMessages = new ArrayList<>(messages);
            
            if (userName != null) {
                // Inject knowledge about the user
                newMessages.add(0, Message.system("The user's name is " + userName + ". Always address them by name."));
            } else {
                newMessages.add(0, Message.system("You don't know the user's name yet. Ask for it politely."));
            }
            return newMessages;
        }

        @Override
        public void afterRun(Object agent, AgentSession session, List<Message> messages, Object response, Map<String, Object> options) {
            // Check the last user message for name
            // We iterate backwards to find the latest user message
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message msg = messages.get(i);
                if (msg.getRole() == Role.USER) {
                    String text = msg.getText();
                    if (text != null && text.toLowerCase().contains("my name is")) {
                        String[] parts = text.toLowerCase().split("my name is");
                        if (parts.length > 1) {
                            String namePart = parts[1].trim();
                            if (!namePart.isEmpty()) {
                                String name = namePart.split(" ")[0];
                                // Capitalize first letter
                                if (!name.isEmpty()) {
                                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                                    session.setMetadata("user_name", name);
                                    System.out.println("[Memory] Stored user name: " + name);
                                }
                            }
                        }
                    }
                    // Only check the latest user message
                    break;
                }
            }
        }
    }
    // </context_provider>

    public static void main(String[] args) {
        // Create a chat client (using OpenAI as example)
        // Ensure you have set the environment variables: MY_OPENAI_API_KEY, MY_OPENAI_BASE_URL, MY_OPENAI_MODEL
        ChatClient client = ClientExample.openAIChatClient();

        Agent agent = AgentBuilder.builder()
                .name("MemoryAgent")
                .instructions("You are a friendly assistant.")
                .client(client)
                .contextProvider(new UserMemoryProvider()) // Add the provider
                .build();

        AgentSession session = agent.createSession();

        System.out.println("Starting memory example...\n");

        // 1. The provider doesn't know the user yet
        runTurn(session, "Hello! What's the square root of 9?");

        // 2. Now provide the name
        runTurn(session, "My name is Alice");

        // 3. Subsequent calls should be personalized
        runTurn(session, "What is 2 + 2?");
        
        // Check metadata
        System.out.println("Session Metadata: " + session.getMetadata("user_name"));
    }

    private static void runTurn(AgentSession session, String input) {
        System.out.println("User: " + input);
        ChatResponse response = session.run(Message.user(input));
        if (response.getMessage() != null) {
             System.out.println("Agent: " + response.getMessage().getText() + "\n");
        } else {
             System.out.println("Agent: [No response text]\n");
        }
    }
}
