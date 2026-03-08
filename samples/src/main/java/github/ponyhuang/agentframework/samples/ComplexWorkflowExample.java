package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
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
                    // This predicate is not used for routing logic in current WorkflowExecutor implementation
                    // The routing logic is in the edges.
                    // But we need this node to be the source of conditional edges.
                    return true; 
                })
                .addAgentNode("tech_agent", techAgent)
                .addAgentNode("general_agent", generalAgent)
                .addEndNode("end", "End")
                
                // Edges
                .startAt("router")
                .addEdge("router", "check_topic")
                // Conditional Edges from 'check_topic'
                .addConditionalEdge("check_topic", "tech_agent", "$is_tech") // Custom condition check
                .addConditionalEdge("check_topic", "general_agent", "$is_general")
                
                .addEdge("tech_agent", "end")
                .addEdge("general_agent", "end")
                .build();

        // Prepare Context with custom condition logic
        // Since WorkflowExecutor's default condition check is very simple (checking boolean in context),
        // we need to wrap the execution or rely on the fact that we can't easily inject logic into edges dynamically 
        // without modifying the executor.
        // HOWEVER, let's look at WorkflowExecutor.evaluateEdgeCondition:
        // if (condition.startsWith("$")) { Object value = context.get(condition.substring(1)); ... }
        // So we need to put "is_tech" or "is_general" into the context BEFORE the edge is evaluated.
        // But the router output is in the messages, not in a boolean variable.
        //
        // WORKAROUND: We can use a Middleware on the Router Agent to set these context variables!
        
        routerAgent.addMiddleware((context, next) -> {
            ChatResponse response = next.apply(context);
            String text = response.getMessage().getText().trim().toUpperCase();
            
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
                    .choices(response.getChoices())
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
        context.put("messages", List.of(Message.user(input)));

        Workflow.Result result = workflow.execute(context);

        if (result.isSuccess()) {
            System.out.println("Workflow finished successfully.");
            // Print the last message
            List<Message> messages = result.getMessages();
            if (messages != null && !messages.isEmpty()) {
                Message last = messages.get(messages.size() - 1);
                System.out.println("Final Answer (" + last.getRole() + "): " + last.getText());
            }
        } else {
            System.out.println("Workflow failed: " + result.getError());
        }
    }
}
