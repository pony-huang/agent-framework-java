package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example demonstrating how to host Agents as a REST API.
 */
public class HostingExample {

    private static final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private static Agent sharedAgent;

    public static void main(String[] args) throws IOException {
        ChatClient client = ClientExample.openAIChatClient();

        Agent agent = AgentBuilder.builder()
                .name("assistant")
                .instructions("You are a helpful assistant.")
                .client(client)
                .build();

        sharedAgent = agent;

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/chat", new ChatHandler(agent));
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on http://localhost:8080/chat");
        System.out.println("Usage: curl -X POST http://localhost:8080/chat -d 'Your message here'");
        System.out.println("With session: curl -X POST 'http://localhost:8080/chat?session_id=my_session' -d 'Your message'");
    }

    private static AgentSession getSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> {
            return sharedAgent.createSession();
        });
    }

    static class ChatHandler implements HttpHandler {
        private final Agent agent;

        ChatHandler(Agent agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Set CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                // Get Session ID
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
                List<Message> collected = session.runStream(session, UserMessage.create(body)).collectList().block();
                ChatResponse response = ChatResponse.builder().messages(collected).build();
                String responseText = response.getMessage().getTextContent();

                System.out.println("[" + sessionId + "] Agent: " + responseText);

                // Send Response
                byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                String error = "Error: " + e.getMessage();
                byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                }
            }
        }
    }
}
