package github.ponyhuang.agentframework.samples.hooks;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.*;
import github.ponyhuang.agentframework.hooks.events.*;
import github.ponyhuang.agentframework.providers.AnthropicChatClient;
import github.ponyhuang.agentframework.types.message.Message;

/**
 * Example demonstrating how to use hooks in the agent framework.
 *
 * This example shows:
 * 1. Programmatic hook registration
 * 2. Command hook handler
 * 3. HTTP hook handler
 * 4. Hook events: SessionStart, Stop, PreToolUse, PostToolUse, PostToolUseFailure
 */
public class HookExample {

    public static void main(String[] args) {
        // Example 1: Programmatic hook registration
        exampleProgrammaticHooks();

        // Example 2: Command hook
        exampleCommandHook();

        // Example 3: HTTP hook
        exampleHttpHook();
    }

    /**
     * Example 1: Programmatic hook registration
     */
    static void exampleProgrammaticHooks() {
        System.out.println("=== Example 1: Programmatic Hooks ===\n");

        // Create hook executor
        HookExecutor hookExecutor = HookExecutor.builder()
                .build();

        // Register hooks programmatically
        hookExecutor.registerHook(HookEvent.SESSION_START, context -> {
            SessionStartContext ctx = (SessionStartContext) context;
            System.out.println("Session started: " + ctx.getSessionId());
            System.out.println("  Source: " + ctx.getSource());
            System.out.println("  Model: " + ctx.getModel());
            return HookResult.allow();
        });  // Match all sources

        hookExecutor.registerHook(HookEvent.STOP, context -> {
            StopContext ctx = (StopContext) context;
            System.out.println("Session stopped");
            System.out.println("  Last message: " +
                    (ctx.getLastAssistantMessage() != null ?
                            ctx.getLastAssistantMessage().substring(0, Math.min(50, ctx.getLastAssistantMessage().length())) : "none"));
            return HookResult.allow();
        });

        hookExecutor.registerHook(HookEvent.PRE_TOOL_USE, context -> {
            PreToolUseContext ctx = (PreToolUseContext) context;
            System.out.println("PreToolUse: " + ctx.getToolName());
            System.out.println("  Input: " + ctx.getToolInput());

            // Example: Block dangerous commands
            if ("Bash".equals(ctx.getToolName())) {
                Object command = ctx.getToolInput().get("command");
                if (command != null && command.toString().contains("rm -rf")) {
                    System.out.println("  BLOCKED: Dangerous command detected!");
                    return HookResult.block("rm -rf commands are not allowed");
                }
            }
            return HookResult.allow();
        }, "Bash");  // Only match Bash tool

        hookExecutor.registerHook(HookEvent.POST_TOOL_USE, context -> {
            PostToolUseContext ctx = (PostToolUseContext) context;
            System.out.println("PostToolUse: " + ctx.getToolName());
            System.out.println("  Success!");
            return HookResult.allow();
        }, "Bash");

        hookExecutor.registerHook(HookEvent.POST_TOOL_USE_FAILURE, context -> {
            PostToolUseFailureContext ctx = (PostToolUseFailureContext) context;
            System.out.println("PostToolUseFailure: " + ctx.getToolName());
            System.out.println("  Error: " + ctx.getError());
            return HookResult.allow();
        }, "Bash");

        System.out.println("Hook executor configured with " +
                hookExecutor.getHookConfigs().values().stream()
                        .mapToInt(java.util.Collection::size).sum() + " hooks\n");
    }

    /**
     * Example 2: Command hook - executes shell commands
     */
    static void exampleCommandHook() {
        System.out.println("=== Example 2: Command Hook ===\n");

        // Create a command hook handler
        // This command will be called with JSON context as stdin
        HookHandler commandHook = CommandHookHandler.builder()
                .command("echo 'Command hook executed' && cat")
                .timeout(java.time.Duration.ofSeconds(10))
                .async(false)
                .build();

        HookExecutor hookExecutor = HookExecutor.builder()
                .build();

        hookExecutor.registerHook(HookEvent.PRE_TOOL_USE, commandHook, "Bash");

        System.out.println("Command hook registered for PreToolUse event on Bash tool\n");
    }

    /**
     * Example 3: HTTP hook - sends HTTP POST requests
     */
    static void exampleHttpHook() {
        System.out.println("=== Example 3: HTTP Hook ===\n");

        // Create an HTTP hook handler
        HookHandler httpHook = HttpHookHandler.builder()
                .url("http://localhost:8080/hooks/pre-tool")
                .timeout(java.time.Duration.ofSeconds(30))
                .async(false)
                .headers(java.util.Map.of(
                        "Authorization", "Bearer $MY_TOKEN",
                        "X-Custom-Header", "value"
                ))
                .allowedEnvVars(java.util.Set.of("MY_TOKEN"))
                .build();

        HookExecutor hookExecutor = HookExecutor.builder()
                .build();

        hookExecutor.registerHook(HookEvent.PRE_TOOL_USE, httpHook, "Read");

        System.out.println("HTTP hook registered for PreToolUse event on Read tool\n");
    }

    /**
     * Example 4: Full agent with hooks - SIMPLE API
     */
    @SuppressWarnings("unused")
    static void exampleFullAgentWithHooks() {
        // Create a ChatClient (replace with your API key)
        ChatClient client = AnthropicChatClient.builder()
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .build();

        // Build agent with hooks - simple and clean!
        Agent agent = AgentBuilder.builder()
                .client(client)
                .instructions("You are a helpful assistant.")
                // Add session start hook
                .hook(HookEvent.SESSION_START, context -> {
                    System.out.println("Agent session started!");
                    return HookResult.allow();
                })
                // Add PreToolUse hook - block dangerous Bash commands
                .hook(HookEvent.PRE_TOOL_USE, context -> {
                    PreToolUseContext ctx = (PreToolUseContext) context;
                    System.out.println("About to execute tool: " + ctx.getToolName());
                    // Example: Block dangerous commands
                    if ("Bash".equals(ctx.getToolName())) {
                        Object cmd = ctx.getToolInput().get("command");
                        if (cmd != null && cmd.toString().contains("rm -rf")) {
                            return HookResult.block("Dangerous command blocked!");
                        }
                    }
                    return HookResult.allow();
                }, "Bash")  // Only match Bash tool
                // Add PostToolUse hook
                .hook(HookEvent.POST_TOOL_USE, context -> {
                    PostToolUseContext ctx = (PostToolUseContext) context;
                    System.out.println("Tool executed: " + ctx.getToolName());
                    return HookResult.allow();
                })
                .build();

        // Run the agent
        // Note: This requires a valid API key to work
        // Message response = agent.run(Message.user("Hello!"));
        // System.out.println(response.getMessage().getText());
    }
}
