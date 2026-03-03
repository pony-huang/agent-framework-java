package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.a2a.agent.A2AAgent;
import github.ponyhuang.agentframework.a2a.agent.A2AContinuationToken;
import github.ponyhuang.agentframework.a2a.client.A2AClient;
import github.ponyhuang.agentframework.a2a.client.A2AClientImpl;
import github.ponyhuang.agentframework.a2a.types.AgentCard;
import github.ponyhuang.agentframework.a2a.types.AgentCapabilities;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;

/**
 * Examples demonstrating how to use A2A (Agent-to-Agent) protocol with agents.
 *
 * <p>A2A is a protocol that enables agents to communicate with each other over HTTP/JSON-RPC.
 * This example shows how to:
 * <ul>
 *   <li>Create an A2A agent from a URL</li>
 *   <li>Create an A2A agent from an AgentCard</li>
 *   <li>Send messages to remote A2A agents</li>
 *   <li>Handle long-running tasks with continuation tokens</li>
 *   <li>Use streaming responses</li>
 * </ul>
 */
public class A2AExample {

    private static A2AServer server;

    public static void main(String[] args) {
        // Start local A2A server first
        try {
            server = new A2AServer(8000);
            server.start();
            // Wait for server to be ready
            Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("Failed to start A2A server: " + e.getMessage());
            System.exit(1);
        }

        try {
            // Example 1: Create agent from URL
            example1CreateAgentFromUrl();

            // Example 2: Create agent from AgentCard (using local server)
            example2CreateAgentFromAgentCard();

            // Example 3: Using continuation token for long-running tasks
            example3ContinuationToken();

            // Example 4: Streaming response
            example4StreamingResponse();
        } finally {
            server.stop();
        }
    }

    /**
     * Example 1: Create A2A Agent from URL
     *
     * The simplest way to create an A2A agent is by providing a URL.
     * The client will automatically resolve the AgentCard from /.well-known/agent.json
     */
    static void example1CreateAgentFromUrl() {
        System.out.println("=== Example 1: Create Agent from URL ===");

        // Create A2A agent from URL
        // This will automatically resolve the AgentCard from the server
        A2AAgent agent = A2AAgent.builder()
                .url("http://localhost:8000")
                .name("remote-agent")
                .build();

        // Get the resolved AgentCard
        AgentCard card = agent.getAgentCard();
        System.out.println("Connected to agent: " + card.getName());
        System.out.println("Description: " + card.getDescription());
        System.out.println("Supports streaming: " + card.supportsStreaming());

        // Send a message
        ChatResponse response = agent.run(java.util.List.of(
                Message.user("Hello, what is your name?")
        ));

        System.out.println("Response: " + response.getMessage().getText());

        // Clean up
        agent.close();
    }

    /**
     * Example 2: Create A2A Agent from AgentCard
     *
     * You can also create an A2A agent by providing an AgentCard directly,
     * or by first creating a client and getting its AgentCard.
     */
    static void example2CreateAgentFromAgentCard() {
        System.out.println("=== Example 2: Create Agent from AgentCard ===");

        // Option 1: Create AgentCard manually (pointing to local server)
        AgentCard manualCard = AgentCard.builder()
                .name("My Custom Agent")
                .description("A custom A2A agent")
                .url("http://localhost:8000")
                .version("1.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .build())
                .build();

        A2AAgent agent1 = A2AAgent.builder()
                .agentCard(manualCard)
                .name("custom-agent")
                .build();

        ChatResponse response1 = agent1.run(java.util.List.of(
                Message.user("Test message")
        ));
        System.out.println("Response from custom agent: " + response1.getMessage().getText());
        agent1.close();

        // Option 2: Resolve AgentCard first, then create agent
        A2AClient client = A2AClientImpl.builder()
                .url("http://localhost:8000")
                .build();

        AgentCard resolvedCard = client.getAgentCard();
        System.out.println("Resolved agent: " + resolvedCard.getName());

        A2AAgent agent2 = A2AAgent.builder()
                .client(client)
                .name("resolved-agent")
                .build();

        ChatResponse response2 = agent2.run(java.util.List.of(
                Message.user("Hello!")
        ));
        System.out.println("Response: " + response2.getMessage().getText());
        agent2.close();
    }

    /**
     * Example 3: Continuation Token for Long-Running Tasks
     *
     * A2A supports long-running tasks. When a task requires more time to complete,
     * you can use a continuation token to poll for the result later.
     */
    static void example3ContinuationToken() {
        System.out.println("=== Example 3: Continuation Token ===");

        A2AAgent agent = A2AAgent.builder()
                .url("http://localhost:8000")
                .name("long-running-agent")
                .build();

        // Send a message that might take a long time
        // In a real scenario, you would check if the response contains a continuation token
        ChatResponse response = agent.run(java.util.List.of(
                Message.user("Process a large dataset")
        ));

        // Check if response has a continuation token (for background mode)
        // In this example, we're showing the basic flow
        System.out.println("Response ID: " + response.getId());
        System.out.println("Response: " + response.getMessage().getText());

        // Example: Poll task if needed
        // A2AContinuationToken token = ...; // obtained from previous response
        // ChatResponse polledResponse = agent.pollTask(token);
        // System.out.println("Polled response: " + polledResponse.getMessage().getText());

        agent.close();
    }

    /**
     * Example 4: Streaming Response
     *
     * A2A supports streaming responses via Server-Sent Events (SSE).
     * You can use runStream() to get incremental updates.
     */
    static void example4StreamingResponse() {
        System.out.println("=== Example 4: Streaming Response ===");

        A2AAgent agent = A2AAgent.builder()
                .url("http://localhost:8000")
                .name("streaming-agent")
                .build();

        // Use runStream for streaming responses
        // This returns a Flux that emits ChatResponse as they arrive
        var flux = agent.runStream(java.util.List.of(
                Message.user("Tell me a long story")
        ));

        // Subscribe to the stream
        flux.subscribe(
                response -> {
                    String text = response.getMessage() != null ? response.getMessage().getText() : "";
                    System.out.println("Stream update: " + text);
                },
                error -> System.err.println("Error: " + error.getMessage()),
                () -> System.out.println("Stream completed")
        );

        // Wait for stream to complete (in real usage, you might want to do this asynchronously)
        try {
            Thread.sleep(5000); // Wait up to 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        agent.close();
    }

    /**
     * Example 5: Minimal Agent Card Fallback
     *
     * If the server doesn't provide an AgentCard, a minimal one will be created.
     */
    static void example5MinimalAgentCard() {
        System.out.println("=== Example 5: Minimal Agent Card Fallback ===");

        // This will create a minimal AgentCard even if /.well-known/agent.json is not available
        A2AAgent agent = A2AAgent.builder()
                .url("http://unknown-server:9999")
                .name("fallback-agent")
                .build();

        AgentCard card = agent.getAgentCard();
        System.out.println("Agent name (fallback): " + card.getName());
        System.out.println("Agent URL: " + card.getUrl());
        System.out.println("Streaming supported: " + card.supportsStreaming());

        agent.close();
    }
}
