package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.HookEvent;
import github.ponyhuang.agentframework.hooks.HookEventBus;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.events.UserPromptSubmitContext;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.message.AssistantMessage;

import java.util.List;

/**
 * Example demonstrating the use of Hooks to intercept and modify agent execution.
 */
public class MiddlewareExample {

    public static void main(String[] args) {
        ChatClient client = ClientExample.openAIChatClient();

        AgentBuilder builder = AgentBuilder.builder()
                .name("HookAgent")
                .instructions("You are a helpful assistant.")
                .client(client)
                .hook(HookEvent.USER_PROMPT_SUBMIT, context -> {
                    if (context instanceof UserPromptSubmitContext promptContext) {
                        String prompt = promptContext.getPrompt();

                        if (prompt != null && prompt.toLowerCase().contains("unsafe")) {
                            System.out.println("[Hook] Blocked unsafe content!");
                            return HookResult.deny("Content blocked by safety hook");
                        }

                        System.out.println("[Hook] Before Agent Run: " + prompt);
                    }
                    return HookResult.allow();
                })
                .hook(HookEvent.STOP, context -> {
                    System.out.println("[Hook] Agent execution completed");
                    return HookResult.allow();
                });

        System.out.println("--- Test 1: Safe Content ---");
        List<Message> messages1 = builder.build().runStream(List.of(UserMessage.create("Hello, how are you?"))).collectList().block();
        ChatResponse response1 = ChatResponse.builder().messages(messages1).build();
        System.out.println("Final Response: " + response1.getMessage().getTextContent() + "\n");

        System.out.println("--- Test 2: Unsafe Content ---");
        List<Message> messages2 = builder.build().runStream(List.of(UserMessage.create("This is unsafe content."))).collectList().block();
        ChatResponse response2 = ChatResponse.builder().messages(messages2).build();
        System.out.println("Final Response: " + (response2.getMessage() != null ? response2.getMessage().getTextContent() : "empty") + "\n");
    }
}
