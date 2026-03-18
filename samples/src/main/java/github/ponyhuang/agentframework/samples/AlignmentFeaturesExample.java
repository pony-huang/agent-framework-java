package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.*;
import github.ponyhuang.agentframework.providers.AnthropicChatClient;
import github.ponyhuang.agentframework.providers.ChatClient;
import github.ponyhuang.agentframework.sessions.DefaultSession;
import github.ponyhuang.agentframework.sessions.Session;
import github.ponyhuang.agentframework.sessions.SessionManager;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Example demonstrating alignment features with Python SDK.
 *
 * Features shown:
 * - maxBudgetUsd: Limit execution costs
 * - fallbackModel: Automatic failover
 * - permissionMode: Tool execution control
 * - disallowedTools: Block specific tools
 * - AgentDefinition: Custom sub-agents
 * - Session fork/resume: Branch conversations
 */
public class AlignmentFeaturesExample {

    public static void main(String[] args) {
        // Create chat client (replace with your API key)
        ChatClient client = AnthropicChatClient.builder()
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .model("claude-sonnet-4-5")
                .build();

        // Example 1: Budget tracking
        demonstrateBudgetTracking(client);

        // Example 2: Permission mode
        demonstratePermissionMode(client);

        // Example 3: Custom agents
        demonstrateCustomAgents(client);

        // Example 4: Session fork
        demonstrateSessionFork(client);
    }

    /**
     * Example 1: Budget tracking - stop when cost exceeds limit
     */
    static void demonstrateBudgetTracking(ChatClient client) {
        System.out.println("=== Budget Tracking Example ===");

        // Create agent with $0.10 budget limit
        LoopAgent agent = LoopAgent.builder()
                .client(client)
                .maxBudgetUsd(0.10)  // Stop at $0.10
                .maxSteps(20)
                .build();

        Session session = new DefaultSession("budget-demo");
        session.start();

        List<Message> messages = List.of(UserMessage.create("Count to 100"));

        // Agent will stop when budget exceeded
        agent.runStream(session, messages).doOnComplete(() -> {
            System.out.println("Completed. Total cost: $" + agent.getCostTracker().getTotalCostUsd());
        }).subscribe(msg -> System.out.println(msg.getTextContent()));
    }

    /**
     * Example 2: Permission mode - PLAN mode blocks tool execution
     */
    static void demonstratePermissionMode(ChatClient client) {
        System.out.println("\n=== Permission Mode Example ===");

        // Create agent in PLAN mode - no tool execution allowed
        LoopAgent planAgent = LoopAgent.builder()
                .client(client)
                .permissionMode(PermissionMode.PLAN)
                .maxSteps(1)
                .build();

        Session session = new DefaultSession("plan-demo");
        session.start();

        List<Message> messages = List.of(UserMessage.create("Create a file called test.txt"));

        // Agent will respond but not execute tools
        planAgent.runStream(session, messages)
                .subscribe(msg -> System.out.println("Response: " + msg.getTextContent()));
    }

    /**
     * Example 3: Custom sub-agents
     */
    static void demonstrateCustomAgents(ChatClient client) {
        System.out.println("\n=== Custom Agents Example ===");

        // Define a code reviewer agent
        AgentDefinition codeReviewer = AgentDefinition.builder()
                .name("code-reviewer")
                .description("Reviews code for bugs and best practices")
                .prompt("You are a code reviewer. Analyze code for bugs, security issues, and best practices.")
                .tools(Set.of("Read", "Grep"))
                .build();

        // Define a documentation agent
        AgentDefinition docsAgent = AgentDefinition.builder()
                .name("docs-writer")
                .description("Generates documentation")
                .prompt("You are a technical writer. Create clear documentation.")
                .tools(Set.of("Read", "Glob", "Write"))
                .build();

        // Create agent with custom sub-agents
        LoopAgent agent = LoopAgent.builder()
                .client(client)
                .agents(Map.of(
                        "code-reviewer", codeReviewer,
                        "docs-writer", docsAgent
                ))
                .maxSteps(1)
                .build();

        System.out.println("Registered agents: " + agent.getAgents().keySet());
        System.out.println("Code reviewer: " + agent.getAgent("code-reviewer").map(AgentDefinition::getDescription).orElse("not found"));
    }

    /**
     * Example 4: Session fork - branch conversations
     */
    static void demonstrateSessionFork(ChatClient client) {
        System.out.println("\n=== Session Fork Example ===");

        SessionManager sessionManager = new SessionManager();
        Session original = sessionManager.createSession();

        LoopAgent agent = LoopAgent.builder()
                .client(client)
                .maxSteps(1)
                .build();

        // Run initial task
        agent.runStream(original, List.of(UserMessage.create("Say hello")))
                .blockFirst();

        System.out.println("Original session: " + original.getMessages().size() + " messages");

        // Fork for experimentation
        Session fork = original.fork();

        // Continue on fork - doesn't affect original
        agent.runStream(fork, List.of(UserMessage.create("Now say goodbye")))
                .blockFirst();

        System.out.println("Fork session: " + fork.getMessages().size() + " messages");
        System.out.println("Original unchanged: " + original.getMessages().size() + " messages");
        System.out.println("Fork parent: " + fork.getParentSessionId());
    }
}
