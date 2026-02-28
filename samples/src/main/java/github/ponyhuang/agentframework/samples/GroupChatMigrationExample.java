package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.orchestrations.GroupChatAgentBuilder;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;

import github.ponyhuang.agentframework.middleware.AgentMiddleware;

import java.util.List;
import java.util.function.Function;

/**
 * Migration Example: Group Chat with Round-Robin Selector
 * 
 * Equivalent to AutoGen's GroupChat with custom speaker selection.
 * Reference: python/samples/03-workflows/orchestrations/group_chat_simple_selector.py
 */
public class GroupChatMigrationExample {

    public static void main(String[] args) {
        ChatClient client = ClientExample.openAIChatClient();

        // Middleware to print conversation trace
        AgentMiddleware loggingMiddleware = (context, next) -> {
            ChatResponse response = next.apply(context);
            System.out.println("\n[" + context.getAgent().getName() + "]: " + response.getMessage().getText());
            return response;
        };

        // 1. Define Agents (Participants)
        Agent expert = AgentBuilder.builder()
                .name("PythonExpert")
                .instructions("You are an expert in Python. Answer questions and refine your answer based on feedback.")
                .client(client)
                .middleware(loggingMiddleware)
                .build();

        Agent verifier = AgentBuilder.builder()
                .name("AnswerVerifier")
                .instructions("You are a programming expert. Review the answer from PythonExpert. Point out dangerous statements. If good, say 'The answer looks good to me.'")
                .client(client)
                .middleware(loggingMiddleware)
                .build();

        Agent clarifier = AgentBuilder.builder()
                .name("AnswerClarifier")
                .instructions("You are an accessibility expert. Review the answer for clarity. Point out jargon. If clear, say 'The answer looks clear to me.'")
                .client(client)
                .middleware(loggingMiddleware)
                .build();

        Agent skeptic = AgentBuilder.builder()
                .name("Skeptic")
                .instructions("You are a devil's advocate. Point out caveats and exceptions. If satisfied, say 'I have no further questions.'")
                .client(client)
                .middleware(loggingMiddleware)
                .build();

        List<Agent> participants = List.of(expert, verifier, clarifier, skeptic);

        // 2. Define Custom Speaker Selector (Round-Robin)
        // AutoGen uses a function `selection_func` or `speaker_selection_method="round_robin"`
        // In Java, we implement a Strategy Function.
        Function<List<Message>, Agent> roundRobinSelector = new Function<>() {
            private int index = 0;
            @Override
            public Agent apply(List<Message> messages) {
                // Determine next speaker based on round index
                // Note: This is a simple stateful selector for this execution instance
                Agent next = participants.get(index % participants.size());
                index++;
                return next;
            }
        };

        // 3. Configure Group Chat Orchestration
        // AutoGen: groupchat = GroupChat(..., max_round=6)
        // Java: GroupChatAgentBuilder
        GroupChatAgentBuilder groupChat = new GroupChatAgentBuilder()
                .participant(expert)
                .participant(verifier)
                .participant(clarifier)
                .participant(skeptic)
                .selectionStrategy(roundRobinSelector) // Use custom selector
                .maxTurns(6); // Stop after 6 turns

        System.out.println("Starting Group Chat: 'How does Python’s Protocol differ from abstract base classes?'\n");

        // 4. Execute
        // AutoGen: user_proxy.initiate_chat(manager, message=...)
        ChatResponse result = groupChat.execute("How does Python’s Protocol differ from abstract base classes?");
        
        System.out.println("\nGroup Chat Finished.");
    }
}
