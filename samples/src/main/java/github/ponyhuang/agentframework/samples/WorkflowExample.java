package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.providers.ChatClient;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.workflows.Workflow;
import github.ponyhuang.agentframework.workflows.WorkflowBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example showing how to use workflows.
 */
public class WorkflowExample {

    public static void main(String[] args) {
        // Create agents for each step
        Agent researchAgent = createAgent("Research Agent", "Research the topic thoroughly.");
        Agent summaryAgent = createAgent("Summary Agent", "Summarize the findings concisely.");

        // Build a workflow
        Workflow workflow = WorkflowBuilder.builder()
                .name("research-workflow")
                .addAgentNode("research", researchAgent)
                .addAgentNode("summarize", summaryAgent)
                .addEdge("research", "summarize")
                .build();

        // Execute workflow
        Map<String, Object> context = new HashMap<>();
        context.put("messages", List.of(UserMessage.create("What is machine learning?")));

        Workflow.Result result = workflow.execute(context);

        System.out.println("Success: " + result.isSuccess());
        if (result.getMessages() != null) {
            for (Message msg : result.getMessages()) {
                if ("assistant".equalsIgnoreCase(msg.getRoleAsString())) {
                    System.out.println("Final: " + msg.getTextContent());
                }
            }
        }
    }

    private static Agent createAgent(String name, String instructions) {
        ChatClient client = ClientExample.anthropicChatClient();

        return AgentBuilder.builder()
                .name(name)
                .instructions(instructions)
                .client(client)
                .build();
    }

}
