package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.HookEvent;
import github.ponyhuang.agentframework.hooks.HookExecutor;
import github.ponyhuang.agentframework.orchestrations.GroupChatAgentBuilder;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;

import java.util.List;
import java.util.function.Function;

/**
 * Migration Example: Group Chat with Round-Robin Selector
 * <p>
 * Equivalent to AutoGen's GroupChat with custom speaker selection.
 */
public class GroupChatMigrationExample {

    public static void main(String[] args) {
        ChatClient client = ClientExample.openAIChatClient();

        HookExecutor hookExecutor = HookExecutor.builder().build();
        hookExecutor.registerHook(HookEvent.SUBAGENT_STOP, context -> {
            if (context instanceof github.ponyhuang.agentframework.hooks.events.SubagentStopContext stopContext) {
                System.out.println("\n[Agent]: " + stopContext.getLastAssistantMessage());
            }
            return github.ponyhuang.agentframework.hooks.HookResult.allow();
        });

        hookExecutor.registerHook(HookEvent.SUBAGENT_START, context -> {
            if (context instanceof github.ponyhuang.agentframework.hooks.events.SubagentStartContext startContext) {
                System.out.println("\n[Subagent Start]: " + startContext.getAgentId());
            }
            return github.ponyhuang.agentframework.hooks.HookResult.allow();
        });

        hookExecutor.registerHook(HookEvent.SUBAGENT_STOP, context -> {
            if (context instanceof github.ponyhuang.agentframework.hooks.events.SubagentStopContext stopContext) {
                System.out.println("[Subagent Stop]: " + stopContext.getAgentId());
            }
            return github.ponyhuang.agentframework.hooks.HookResult.allow();
        });

        Agent expert = AgentBuilder.builder()
                .name("PythonExpert")
                .instructions("You are an expert in Python. Answer questions and refine your answer based on feedback.")
                .client(client)
                .hookExecutor(hookExecutor)
                .build();

        Agent verifier = AgentBuilder.builder()
                .name("AnswerVerifier")
                .instructions("You are a programming expert. Review the answer from PythonExpert. Point out dangerous statements. If good, say 'The answer looks good to me.'")
                .client(client)
                .hookExecutor(hookExecutor)
                .build();

        Agent clarifier = AgentBuilder.builder()
                .name("AnswerClarifier")
                .instructions("You are an accessibility expert. Review the answer for clarity. Point out jargon. If clear, say 'The answer looks clear to me.'")
                .client(client)
                .hookExecutor(hookExecutor)
                .build();

        Agent skeptic = AgentBuilder.builder()
                .name("Skeptic")
                .instructions("You are a devil's advocate. Point out caveats and exceptions. If satisfied, say 'I have no further questions.'")
                .client(client)
                .hookExecutor(hookExecutor)
                .build();

        List<Agent> participants = List.of(expert, verifier, clarifier, skeptic);

        Function<List<Message>, Agent> roundRobinSelector = new Function<>() {
            private int index = 0;

            @Override
            public Agent apply(List<Message> messages) {
                Agent next = participants.get(index % participants.size());
                index++;
                return next;
            }
        };

        GroupChatAgentBuilder groupChat = new GroupChatAgentBuilder()
                .participant(expert)
                .participant(verifier)
                .participant(clarifier)
                .participant(skeptic)
                .selectionStrategy(roundRobinSelector)
                .maxTurns(6);

        System.out.println("Starting Group Chat: 'How does Python's Protocol differ from abstract base classes?'\n");

        ChatResponse result = groupChat.execute("How does Python's Protocol differ from abstract base classes?");

        System.out.println("\nGroup Chat Finished.");
    }
}
