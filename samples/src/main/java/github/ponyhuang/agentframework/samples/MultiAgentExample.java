package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.orchestrations.*;
import github.ponyhuang.agentframework.types.ChatResponse;

/**
 * Example showing different multi-agent orchestration patterns.
 */
public class MultiAgentExample {

    public static void main(String[] args) {
        // Create agents
        Agent analyzer = createAgent("Analyzer", "Analyze data carefully.");
        Agent writer = createAgent("Writer", "Write clear summaries.");
        Agent editor = createAgent("Editor", "Edit and improve content.");

        // Example 1: Sequential
        System.out.println("=== Sequential ===");
        ChatResponse sequentialResult = new SequentialAgentBuilder()
                .agent(analyzer)
                .agent(writer)
                .execute("Explain quantum computing");
        System.out.println(sequentialResult.getMessage().getTextContent());

        // Example 2: Concurrent
        System.out.println("\n=== Concurrent ===");
        var concurrentBuilder = new ConcurrentAgentBuilder()
                .agent(analyzer)
                .agent(writer)
                .agent(editor);
        var responses = concurrentBuilder.execute("Review this code");
        responses.forEach(r -> System.out.println(r.getMessage().getTextContent()));

        // Example 3: Handoff
        System.out.println("\n=== Handoff ===");
        var handoffBuilder = new HandoffAgentBuilder()
                .agent("analyzer", analyzer)
                .agent("writer", writer)
                .handoffDecider(response -> {
                    String text = response.getMessage().getTextContent().toLowerCase();
                    if (text.contains("need more details")) return "analyzer";
                    if (text.contains("ready to write")) return "writer";
                    return null;
                });
        ChatResponse handoffResult = handoffBuilder.execute("Start the task");
        System.out.println(handoffResult.getMessage().getTextContent());

        // Example 4: Group Chat
        System.out.println("\n=== Group Chat ===");
        ChatResponse groupResult = new GroupChatAgentBuilder()
                .participant(analyzer)
                .participant(writer)
                .participant(editor)
                .moderator(editor)
                .maxTurns(5)
                .execute("Create a short story");
        System.out.println(groupResult.getMessage().getTextContent());

        // Example 5: Magentic One
        System.out.println("\n=== Magentic One ===");
        Agent orchestrator = createAgent("Orchestrator", "Coordinate the team.");
        var magenticBuilder = new MagenticAgentBuilder()
                .orchestrator(orchestrator)
                .specialist("analyzer", analyzer)
                .specialist("writer", writer)
                .specialist("editor", editor)
                .maxIterations(10);
        ChatResponse magenticResult = magenticBuilder.execute("Write a comprehensive report");
        System.out.println(magenticResult.getMessage().getTextContent());
    }

    private static Agent createAgent(String name, String instructions) {
        ChatClient client = ClientExample.openAIChatClient();

        return AgentBuilder.builder()
                .name(name)
                .instructions(instructions)
                .client(client)
                .build();
    }
}
