package github.ponyhuang.agentframework.samples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Simple local A2A server for testing purposes.
 *
 * This server implements the A2A (Agent-to-Agent) protocol and provides:
 * - GET /.well-known/agent.json - Returns AgentCard
 * - POST / - Handles JSON-RPC requests (tasks/send)
 */
public class A2AServer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final int port;

    public A2AServer(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/.well-known/agent.json", this::handleAgentCard);
        server.createContext("/", this::handleJsonRpc);

        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
        System.out.println("A2A Server started at http://localhost:" + port);
    }

    public void stop() {
        server.stop(0);
        System.out.println("A2A Server stopped");
    }

    private void handleAgentCard(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        Map<String, Object> agentCard = Map.of(
                "name", "Demo Assistant",
                "description", "A simple A2A demo agent that responds to messages",
                "url", "http://localhost:" + port,
                "version", "1.0.0",
                "capabilities", Map.of("streaming", true)
        );

        sendJson(exchange, 200, agentCard);
    }

    private void handleJsonRpc(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        String requestBody = new String(exchange.getRequestBody().readAllBytes());
        Map<String, Object> request = MAPPER.readValue(requestBody, Map.class);

        String method = (String) request.get("method");
        String id = (String) request.get("id");

        Map<String, Object> params = (Map<String, Object>) request.get("params");

        // Handle tasks/send method
        if ("tasks/send".equals(method)) {
            handleTasksSend(exchange, params, id);
        } else {
            sendJson(exchange, 200, Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of("code", -32601, "message", "Method not found"),
                    "id", id
            ));
        }
    }

    private void handleTasksSend(HttpExchange exchange, Map<String, Object> params, String id) throws IOException {
        // Get the message from params
        Map<String, Object> message = (Map<String, Object>) params.get("message");
        String userText = "";

        if (message != null) {
            List<Map<String, Object>> contents = (List<Map<String, Object>>) message.get("content");
            if (contents != null && !contents.isEmpty()) {
                Map<String, Object> content = contents.get(0);
                if ("text".equals(content.get("type"))) {
                    userText = (String) content.get("text");
                }
            }
        }

        // Generate a simple response
        String responseText = generateResponse(userText);

        // Build the response following A2A protocol
        // Use Jackson polymorphic type format (kind wrapper)
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("kind", "text");
        textPart.put("text", responseText);
        textPart.put("metadata", null);

        Map<String, Object> responseMessage = Map.of(
                "role", "AGENT",
                "parts", List.of(textPart)
        );

        Map<String, Object> task = new HashMap<>();
        task.put("id", UUID.randomUUID().toString());
        task.put("status", Map.of("state", "COMPLETED"));
        task.put("history", List.of());
        task.put("messages", List.of(responseMessage));

        Map<String, Object> result = Map.of("task", task);

        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);

        sendJson(exchange, 200, response);
    }

    private String generateResponse(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return "Hello! How can I help you today?";
        }

        String lowerInput = userInput.toLowerCase();

        if (lowerInput.contains("hello") || lowerInput.contains("hi")) {
            return "Hello! I'm your A2A demo agent. Nice to meet you!";
        }

        if (lowerInput.contains("name")) {
            return "My name is Demo Assistant. I'm a simple A2A protocol demo.";
        }

        if (lowerInput.contains("how are you")) {
            return "I'm doing great, thank you for asking! How can I assist you?";
        }

        if (lowerInput.contains("help")) {
            return "I can answer simple questions and have basic conversations. Try asking me about my name or how I'm doing!";
        }

        return "I received your message: \"" + userInput + "\". This is a demo response from the A2A server.";
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        A2AServer server = new A2AServer(port);
        server.start();

        System.out.println("Press Ctrl+C to stop the server");

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}