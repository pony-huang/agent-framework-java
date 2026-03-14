package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.providers.ChatClient;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.event.HookEventType;
import github.ponyhuang.agentframework.hooks.event.SubagentStartEvent;
import github.ponyhuang.agentframework.hooks.event.SubagentStopEvent;
import github.ponyhuang.agentframework.orchestrations.GroupChatAgentBuilder;
import github.ponyhuang.agentframework.types.ChatResponse;

import java.util.function.Function;

/**
 * Migration Example: Group Chat with Round-Robin Selector
 * <p>
 * Equivalent to AutoGen's GroupChat with custom speaker selection.
 */
public class GroupChatMigrationExample {

    public static void main(String[] args) {
        ChatClient client = ClientExample.openAIChatClient();

        Function<github.ponyhuang.agentframework.hooks.event.BaseEvent, HookResult> stop = event -> {
            if (event instanceof SubagentStopEvent stopEvent) {
                System.out.println("\n[Agent]: " + stopEvent.getLastAssistantMessage());
            }
            return HookResult.allow();
        };

        Function<github.ponyhuang.agentframework.hooks.event.BaseEvent, HookResult> subStart = event -> {
            if (event instanceof SubagentStartEvent startEvent) {
                System.out.println("\n[Subagent Start]: " + startEvent.getAgentId());
            }
            return HookResult.allow();
        };

        Function<github.ponyhuang.agentframework.hooks.event.BaseEvent, HookResult> subStop = event -> {
            if (event instanceof SubagentStopEvent stopEvent) {
                System.out.println("[Subagent Stop]: " + stopEvent.getAgentId());
            }
            return HookResult.allow();
        };

        Agent javaExpert = AgentBuilder.builder()
                .name("JavaExpert")
                .instructions("You are a Java expert.")
                .client(client)
                .build();

        Agent pythonExpert = AgentBuilder.builder()
                .name("PythonExpert")
                .instructions("You are a Python expert.")
                .client(client)
                .build();

        Agent general = AgentBuilder.builder()
                .name("GeneralAssistant")
                .instructions("You are a general assistant.")
                .client(client)
                .hook(HookEventType.STOP, stop)
                .hook(HookEventType.SESSION_START, subStart)
                .hook(HookEventType.SUBAGENT_STOP, subStop)
                .build();


        GroupChatAgentBuilder groupChat = new GroupChatAgentBuilder()
                .participant(javaExpert)
                .participant(pythonExpert)
                .moderator(general)
                .maxTurns(6);

        // Run the group chat
        String input = "Hello, can you help me with Python? And also tell me a joke.";
        System.out.println("User: " + input);
        ChatResponse response = groupChat.execute(input);
        System.out.println("\n=== Response ===");
        System.out.println(response.getLastMessage().getTextContent());
    }
}
