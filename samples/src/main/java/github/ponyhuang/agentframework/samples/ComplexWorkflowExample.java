package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.workflows.Workflow;
import github.ponyhuang.agentframework.workflows.WorkflowBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating a complex workflow with branching.
 *
 * Workflow:
 * 1. Router Agent: Analyzes the user request and decides the topic.
 * 2. Condition Node: Routes based on the topic (Tech or General).
 * 3. Tech Agent: Handles technical questions.
 * 4. General Agent: Handles general questions.
 */
public class ComplexWorkflowExample {

    public static void main(String[] args) {
        ChatClient client = ClientExample.openAIChatClient();

        // 1. Router Agent
        Agent routerAgent = AgentBuilder.builder()
                .name("Router")
                .instructions("You are a router. Analyze the user's input. If it is about programming or technology, reply with 'TECH'. Otherwise, reply with 'GENERAL'. Do not add any other text.")
                .client(client)
                .build();

        // 2. Tech Agent
        Agent techAgent = AgentBuilder.builder()
                .name("TechAgent")
                .instructions("You are a technical expert. Answer the question with code examples if possible.")
                .client(client)
                .build();

        // 3. General Agent
        Agent generalAgent = AgentBuilder.builder()
                .name("GeneralAgent")
                .instructions("You are a general assistant. Answer politely.")
                .client(client)
                .build();

        // Build Workflow
        Workflow workflow = WorkflowBuilder.builder()
                .name("RoutingWorkflow")
                .addAgentNode("router", routerAgent)
                .addConditionNode("check_topic", "Check Topic", context -> {
                    return true;
                })
                .addAgentNode("tech_agent", techAgent)
                .addAgentNode("general_agent", generalAgent)
                .addEndNode("end", "End")

                // Edges
                .startAt("router")
                .addEdge("router", "check_topic")
                // Conditional Edges from 'check_topic'
                .addConditionalEdge("check_topic", "tech_agent", "$is_tech")
                .addConditionalEdge("check_topic", "general_agent", "$is_general")

                .addEdge("tech_agent", "end")
                .addEdge("general_agent", "end")
                .build();

        routerAgent.addMiddleware((context, next) -> {
            ChatResponse response = next.apply(context);
            String text = response.getMessage().getTextContent().trim().toUpperCase();

            // Create a new map for extra properties
            Map<String, Object> extra = new HashMap<>();
            if (response.getExtraProperties() != null) {
                extra.putAll(response.getExtraProperties());
            }

            if (text.contains("TECH")) {
                extra.put("is_tech", true);
                extra.put("is_general", false);
            } else {
                extra.put("is_tech", false);
                extra.put("is_general", true);
            }

            // Return response with new extra properties
            return ChatResponse.builder()
                    .id(response.getId())
                    .created(response.getCreated())
                    .model(response.getModel())
                    .messages(response.getMessages())
                    .usage(response.getUsage())
                    .finishReason(response.getFinishReason())
                    .extraProperties(extra)
                    .build();
        });

        System.out.println("--- Test 1: Technical Question ---");
        runWorkflow(workflow, "How do I write a loop in Java?");

        System.out.println("\n--- Test 2: General Question ---");
        runWorkflow(workflow, "What is the capital of France?");
    }

    private static void runWorkflow(Workflow workflow, String input) {
        Map<String, Object> context = new HashMap<>();
        context.put("messages", List.of(UserMessage.create(input)));

        Workflow.Result result = workflow.execute(context);

        if (result.isSuccess()) {
            System.out.println("Workflow finished successfully.");
            // Print the last message
            List<Message> messages = result.getMessages();
            if (messages != null && !messages.isEmpty()) {
                Message last = messages.get(messages.size() - 1);
                System.out.println("Final Answer (" + last.getRoleAsString() + "): " + last.getTextContent());
            }
        } else {
            System.out.println("Workflow failed: " + result.getError());
        }
    }
}
