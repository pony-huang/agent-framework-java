package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.event.HookEventType;
import github.ponyhuang.agentframework.hooks.event.StopEvent;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.workflows.Workflow;
import github.ponyhuang.agentframework.workflows.WorkflowBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example showing complex workflow with routing.
 */
public class ComplexWorkflowExample {

    private static final Map<String, Object> workflowContext = new HashMap<>();

    public static void main(String[] args) {
        ChatClient client = ClientExample.openAIChatClient();

        // Build router agent with hook for stopping
        Agent routerAgent = AgentBuilder.builder()
                .name("Router")
                .instructions("You are a router. Analyze the user's input. If it is about programming or technology, reply with 'TECH'. Otherwise, reply with 'GENERAL'. Do not add any other text.")
                .client(client)
                .hook(HookEventType.STOP, event -> {
                    if (event instanceof StopEvent stopEvent) {
                        String text = stopEvent.getLastAssistantMessage();
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
                })
                .build();

        // Build specialized agents
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

        // Build workflow with router
        Workflow routerWorkflow = WorkflowBuilder.builder()
                .addAgentNode("routerAgent", routerAgent)
                .build();

        // Simple routing logic
//        String input = "How do I write a loop in Java?";
//        List<Message> messages = List.of(UserMessage.create(input));
//
//        ChatResponse response = routerAgent.runStream(messages);
//        String routerOutput = response.getMessage() != null ? response.getMessage().getTextContent() : "";
//
//        System.out.println("Router output: " + routerOutput);
//
//        // Route to appropriate agent based on router output
//        Agent selectedAgent = routerOutput.toUpperCase().contains("TECH") ? techAgent : generalAgent;
//
//        ChatResponse finalResponse = selectedAgent.run(messages, null);
//        System.out.println("Final response from " + selectedAgent.getName() + ": " +
//                (finalResponse.getMessage() != null ? finalResponse.getMessage().getTextContent() : ""));
//
//        // Display workflow context
//        System.out.println("Workflow context: " + workflowContext);
    }
}