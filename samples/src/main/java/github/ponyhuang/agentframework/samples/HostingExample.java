package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example demonstrating how to host Agents as a REST API.
 * This simulates an Azure Functions or Spring Boot environment.
 * 
 * Endpoints:
 * - POST /agent/chat?session_id={id} : Synchronous chat
 * - POST /agent/stream?session_id={id} : Streaming chat (SSE)
 */
public class HostingExample {

    // 1. Agent Registry: Manages agent instances
    private static final Map<String, Agent> agentRegistry = new HashMap<>();
    
    // 2. Session Store: Manages conversation state (In-memory for demo)
    // In production (Azure Functions), use CosmosDB, Redis, or Durable Entities.
    private static final Map<String, AgentSession> sessionStore = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        // Initialize Agents
        initializeAgents();

        // Start HTTP Server
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/agent/chat", new ChatHandler());
        server.createContext("/agent/stream", new StreamHandler());
        
        server.setExecutor(null); // Default executor
        server.start();
        
        System.out.println("Agent Host Server started on port " + port);
        System.out.println("Try sending a POST request to http://localhost:" + port + "/agent/chat?session_id=user1 with body 'Hello'");
    }

    private static void initializeAgents() {
        ChatClient client = ClientExample.openAIChatClient();
        
        Agent defaultAgent = AgentBuilder.builder()
                .name("assistant")
                .instructions("You are a helpful HTTP assistant.")
                .client(client)
                .build();
                
        agentRegistry.put("default", defaultAgent);
        System.out.println("Initialized agent: default");
    }

    // Helper to get or create session
    private static AgentSession getSession(String sessionId) {
        return sessionStore.computeIfAbsent(sessionId, id -> {
            Agent agent = agentRegistry.get("default");
            System.out.println("Creating new session for: " + id);
            return agent.createSession();
        });
    }

    // Handler for synchronous chat
    static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // Parse Query Params
            String query = exchange.getRequestURI().getQuery();
            String sessionId = "default_session";
            if (query != null && query.contains("session_id=")) {
                sessionId = query.split("session_id=")[1].split("&")[0];
            }

            // Read Body
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("[" + sessionId + "] User: " + body);

            // Execute Agent
            AgentSession session = getSession(sessionId);
            ChatResponse response = session.run(Message.user(body));
            String responseText = response.getMessage().getText();
            
            System.out.println("[" + sessionId + "] Agent: " + responseText);

            // Send Response
            byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    // Handler for streaming chat (SSE)
    static class StreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String sessionId = "default_session";
            if (query != null && query.contains("session_id=")) {
                sessionId = query.split("session_id=")[1].split("&")[0];
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("[" + sessionId + "] User (Stream): " + body);

            AgentSession session = getSession(sessionId);

            // Set headers for SSE
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                // Subscribe to Flux and write to output stream
                session.runStream(Message.user(body))
                        .doOnNext(response -> {
                            String text = response.getMessage().getText(); // This might be partial text
                            // Note: In real streaming, ChatResponse should return delta. 
                            // Assuming ChatClient implementation returns deltas for streaming.
                            if (text != null && !text.isEmpty()) {
                                try {
                                    String data = "data: " + text + "\n\n";
                                    os.write(data.getBytes(StandardCharsets.UTF_8));
                                    os.flush();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        })
                        .blockLast(); // Wait for completion
                
                // End of stream
                os.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
