package github.ponyhuang.agentframework.a2a.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.ponyhuang.agentframework.a2a.A2AException;
import github.ponyhuang.agentframework.a2a.client.A2AEvent;
import github.ponyhuang.agentframework.a2a.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class A2AClientImpl implements A2AClient {
    private static final Logger LOG = LoggerFactory.getLogger(A2AClientImpl.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern DATA_PREFIX = Pattern.compile("^data:\\s*");
    private static final String WELL_KNOWN_AGENT = "/.well-known/agent.json";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AgentCard agentCard;
    private final boolean closeHttpClient;

    private A2AClientImpl(Builder builder) {
        this.baseUrl = builder.url;
        this.objectMapper = builder.objectMapper != null ? builder.objectMapper : MAPPER;
        this.closeHttpClient = builder.httpClient == null;

        if (builder.httpClient != null) {
            this.httpClient = builder.httpClient;
        } else {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }

        if (builder.agentCard != null) {
            this.agentCard = builder.agentCard;
        } else {
            this.agentCard = resolveAgentCard();
        }
    }

    private AgentCard resolveAgentCard() {
        try {
            String agentUrl = baseUrl.endsWith("/") ? baseUrl + WELL_KNOWN_AGENT : baseUrl + WELL_KNOWN_AGENT;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), AgentCard.class);
            }
            LOG.warn("Failed to resolve AgentCard, status: {}", response.statusCode());
            return AgentCard.createMinimal(baseUrl);
        } catch (Exception e) {
            LOG.warn("Failed to resolve AgentCard, using minimal: {}", e.getMessage());
            return AgentCard.createMinimal(baseUrl);
        }
    }

    @Override
    public AgentCard getAgentCard() {
        return agentCard;
    }

    @Override
    public Flux<A2AEvent> sendMessage(Message message) {
        return Flux.create(sink -> {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("message", message);

                JsonRpcRequest request = JsonRpcRequest.builder()
                        .method("tasks/send")
                        .params(params)
                        .build();

                String requestBody = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    sink.error(A2AException.transportError("HTTP " + response.statusCode() + ": " + response.body()));
                    return;
                }

                String body = response.body();
                if (body.contains("text/event-stream") || body.startsWith("data:")) {
                    handleSseStream(body, sink);
                } else {
                    handleJsonResponse(body, sink);
                }

                sink.complete();
            } catch (Exception e) {
                sink.error(A2AException.transportError("Failed to send message", e));
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private void handleSseStream(String body, FluxSink<A2AEvent> sink) {
        String[] lines = body.split("\n");
        for (String line : lines) {
            if (line.startsWith("data:")) {
                String data = DATA_PREFIX.matcher(line).replaceFirst("").trim();
                if (!data.isEmpty() && !data.equals("[DONE]")) {
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        A2AEvent event = parseEvent(node);
                        if (event != null) {
                            sink.next(event);
                        }
                    } catch (JsonProcessingException e) {
                        LOG.warn("Failed to parse SSE data: {}", e.getMessage());
                    }
                }
            }
        }
    }

    private void handleJsonResponse(String body, FluxSink<A2AEvent> sink) throws JsonProcessingException {
        JsonRpcResponse rpcResponse = objectMapper.readValue(body, JsonRpcResponse.class);
        if (rpcResponse.isError()) {
            sink.error(A2AException.taskError("JSON-RPC error: " + rpcResponse.getError().getMessage()));
            return;
        }

        Object result = rpcResponse.getResult();
        if (result == null) {
            return;
        }

        // Handle both JsonNode and LinkedHashMap (from Jackson deserialization)
        if (result instanceof JsonNode) {
            A2AEvent event = parseEvent((JsonNode) result);
            if (event != null) {
                sink.next(event);
            }
        } else if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            if (resultMap.containsKey("task")) {
                Task task = objectMapper.convertValue(resultMap.get("task"), Task.class);
                sink.next(task);
            } else if (resultMap.containsKey("message")) {
                github.ponyhuang.agentframework.a2a.types.Message message =
                        objectMapper.convertValue(resultMap.get("message"), github.ponyhuang.agentframework.a2a.types.Message.class);
                sink.next(message);
            }
        }
    }

    private A2AEvent parseEvent(JsonNode node) {
        if (node.has("result")) {
            JsonNode result = node.get("result");
            if (result.has("task")) {
                return (A2AEvent) objectMapper.convertValue(result.get("task"), Task.class);
            } else if (result.has("message")) {
                return (A2AEvent) objectMapper.convertValue(result.get("message"), github.ponyhuang.agentframework.a2a.types.Message.class);
            }
        }
        return null;
    }

    @Override
    public Task getTask(String taskId) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id", taskId);

            JsonRpcRequest request = JsonRpcRequest.builder()
                    .method("tasks/get")
                    .params(params)
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw A2AException.transportError("HTTP " + response.statusCode());
            }

            JsonRpcResponse rpcResponse = objectMapper.readValue(response.body(), JsonRpcResponse.class);
            if (rpcResponse.isError()) {
                throw A2AException.taskError(rpcResponse.getError().getMessage());
            }

            JsonNode result = (JsonNode) rpcResponse.getResult();
            return objectMapper.convertValue(result.get("task"), Task.class);
        } catch (JsonProcessingException e) {
            throw A2AException.transportError("Failed to parse task response", e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw A2AException.transportError("Timeout getting task", e);
        } catch (java.io.IOException e) {
            throw A2AException.transportError("IO error getting task", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw A2AException.transportError("Interrupted while getting task", e);
        }
    }

    @Override
    public Flux<A2AEvent> resubscribe(String taskId) {
        return Flux.create(sink -> {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("id", taskId);

                JsonRpcRequest request = JsonRpcRequest.builder()
                        .method("tasks/resubscribe")
                        .params(params)
                        .build();

                String requestBody = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    sink.error(A2AException.transportError("HTTP " + response.statusCode()));
                    return;
                }

                String body = response.body();
                if (body.contains("text/event-stream") || body.startsWith("data:")) {
                    handleSseStream(body, sink);
                } else {
                    handleJsonResponse(body, sink);
                }

                sink.complete();
            } catch (Exception e) {
                sink.error(A2AException.transportError("Failed to resubscribe", e));
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public void close() {
        if (closeHttpClient) {
            LOG.debug("A2AClient closing HTTP client");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private AgentCard agentCard;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder agentCard(AgentCard agentCard) {
            this.agentCard = agentCard;
            return this;
        }

        public A2AClientImpl build() {
            if (url == null && agentCard == null) {
                throw new IllegalArgumentException("Either url or agentCard must be provided");
            }
            if (url == null && agentCard != null) {
                url = agentCard.getUrl();
            }
            return new A2AClientImpl(this);
        }
    }
}
