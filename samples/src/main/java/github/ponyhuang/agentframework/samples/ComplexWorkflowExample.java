package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.HookEvent;
import github.ponyhuang.agentframework.hooks.HookExecutor;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.events.StopContext;
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

    private static Map<String, Object> workflowContext = new HashMap<>();

    public static void main(String[] args) {
        ChatClient client = ClientExample.openAIChatClient();

        HookExecutor hookExecutor = HookExecutor.builder().build();
        hookExecutor.registerHook(HookEvent.STOP, context -> {
            if (context instanceof StopContext stopContext) {
                String text = stopContext.getLastAssistantMessage();
                if (text != null) {
                    text = text.toUpperCase();
                    if (text.contains("TECH")) {
                        workflowContext.put("is_tech", true);
                        workflowContext.put("is_general", false);
                    } else {
                        workflowContext.put("is_tech", false);
                        workflowContext.put("is_general", true);
                    }
                }
            }
            return HookResult.allow();
        });

        Agent routerAgent = AgentBuilder.builder()
                .name("Router")
                .instructions("You are a router. Analyze the user's input. If it is about programming or technology, reply with 'TECH'. Otherwise, reply with 'GENERAL'. Do not add any other text.")
                .client(client)
                .hookExecutor(hookExecutor)
                .build();

        Agent techAgent = AgentBuilder.builder()
                .name("TechAgent")
                .instructions("You are a technical expert. Answer the question with code examples if possible.")
                .client(client)
                .build();

        Agent generalAgent = AgentBuilder.builder()
                .name("GeneralAgent")
                .instructions("You are a general assistant. Answer politely.")
                .client(client)
                .build();

        Workflow workflow = WorkflowBuilder.builder()
                .name("RoutingWorkflow")
                .addAgentNode("router", routerAgent)
                .addConditionNode("check_topic", "Check Topic", context -> {
                    return (Boolean) workflowContext.getOrDefault("is_tech", false);
                })
                .addAgentNode("tech_agent", techAgent)
                .addAgentNode("general_agent", generalAgent)
                .addEndNode("end", "End")

                .startAt("router")
                .addEdge("router", "check_topic")
                .addConditionalEdge("check_topic", "tech_agent", "$is_tech")
                .addConditionalEdge("check_topic", "general_agent", "$is_general")

                .addEdge("tech_agent", "end")
                .addEdge("general_agent", "end")
                .build();

        System.out.println("--- Test 1: Technical Question ---");
        workflowContext.clear();
        runWorkflow(workflow, "How do I write a loop in Java?");

        System.out.println("\n--- Test 2: General Question ---");
        workflowContext.clear();
        runWorkflow(workflow, "What is the capital of France?");
    }

    private static void runWorkflow(Workflow workflow, String input) {
        Map<String, Object> context = new HashMap<>();
        context.put("messages", List.of(UserMessage.create(input)));

        Workflow.Result result = workflow.execute(context);

        if (result.isSuccess()) {
            System.out.println("Workflow finished successfully.");
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
