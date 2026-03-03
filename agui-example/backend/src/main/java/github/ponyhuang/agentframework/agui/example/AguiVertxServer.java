package github.ponyhuang.agentframework.agui.example;

import com.agui.core.agent.Agent;
import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.*;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.message.AssistantMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.ponyhuang.agentframework.agui.agent.AguiAgent;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.providers.OpenAIChatClient;
import github.ponyhuang.agentframework.tools.FunctionTool;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AguiVertxServer extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(AguiVertxServer.class);

    private final Agent agent;
    private final ObjectMapper objectMapper;

    // Session storage: sessionId -> List of messages
    private final Map<String, List<BaseMessage>> sessions = new ConcurrentHashMap<>();

    public AguiVertxServer(Agent agent) {
        this.agent = agent;
        this.objectMapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        ChatClient client = OpenAIChatClient.builder()
                .apiKey(System.getenv("MY_OPENAI_API_KEY"))
                .baseUrl(System.getenv("MY_OPENAI_BASE_URL"))
                .model(System.getenv("MY_OPENAI_MODEL"))
                .build();

        Map<String, Object> calcSchema = new HashMap<>();
        calcSchema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        Map<String, Object> exprProp = new HashMap<>();
        exprProp.put("type", "string");
        exprProp.put("description", "The mathematical expression to evaluate");
        props.put("expression", exprProp);
        calcSchema.put("properties", props);
        calcSchema.put("required", List.of("expression"));

        FunctionTool calculator = FunctionTool.builder()
                .name("calculator")
                .description("Calculate a mathematical expression")
                .schema(calcSchema)
                .invoker(new FunctionTool.ToolInvoker() {
                    @Override
                    public Object invoke(Map<String, Object> params) {
                        String expr = (String) params.get("expression");
                        try {
                            return evaluate(expr);
                        } catch (Exception e) {
                            return "Error: " + e.getMessage();
                        }
                    }

                    private double evaluate(String expr) {
                        expr = expr.replaceAll("\\s+", "");
                        return evaluateExpression(expr.toCharArray(), 0)[0];
                    }

                    private double[] evaluateExpression(char[] expr, int pos) {
                        double[] result = new double[2];
                        double num = 0;
                        double sign = 1;
                        char op = '+';

                        while (pos < expr.length) {
                            char c = expr[pos];
                            if (Character.isDigit(c) || c == '.') {
                                StringBuilder sb = new StringBuilder();
                                while (pos < expr.length && (Character.isDigit(expr[pos]) || expr[pos] == '.')) {
                                    sb.append(expr[pos++]);
                                }
                                num = Double.parseDouble(sb.toString());
                            } else if (c == '-') {
                                sign = -1;
                            } else if (c == '(') {
                                double[] sub = evaluateExpression(expr, pos + 1);
                                num = sub[0];
                                pos = (int) sub[1];
                            } else if (c == ')') {
                                result[0] = applyOp(result[0], op, num * sign);
                                result[1] = pos + 1;
                                return result;
                            } else if (c == '+' || c == '*' || c == '/') {
                                result[0] = applyOp(result[0], op, num * sign);
                                op = c;
                                sign = 1;
                                num = 0;
                            } else if (c == '^') {
                                double[] sub = evaluateExpression(expr, pos + 1);
                                num = Math.pow(num, sub[0]);
                                pos = (int) sub[1];
                            }
                            pos++;
                        }
                        result[0] = applyOp(result[0], op, num * sign);
                        result[1] = pos;
                        return result;
                    }

                    private double applyOp(double a, char op, double b) {
                        return switch (op) {
                            case '+' -> a + b;
                            case '-' -> a - b;
                            case '*' -> a * b;
                            case '/' -> b != 0 ? a / b : Double.NaN;
                            default -> b;
                        };
                    }
                })
                .build();

        Agent aguiAgent = AguiAgent.builder()
                .name("calculator-agent")
                .instructions("You are a helpful assistant that uses the calculator tool for math operations.")
                .client(client)
                .addTool(calculator)
                .maxSteps(10)
                .terminationHandler("task_done"::equals)
                .build();

        io.vertx.core.Vertx vertx = io.vertx.core.Vertx.vertx();
        vertx.deployVerticle(new AguiVertxServer(aguiAgent));
    }

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer(new HttpServerOptions());

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.get("/health").handler(this::handleHealth);
        router.post("/agent/run").handler(this::handleRunAgent);

        router.get("/").handler(rc -> {
            rc.response().putHeader("Location", "/index.html").setStatusCode(302).end();
        });

        router.get("/index.html").handler(rc -> {
            try {
                String content = new String(getClass().getResourceAsStream("/frontend/index.html").readAllBytes());
                rc.response()
                        .putHeader("Content-Type", "text/html")
                        .end(content);
            } catch (Exception e) {
                rc.response().setStatusCode(500).end("Error: " + e.getMessage());
            }
        });

        server.requestHandler(router).listen(8080, result -> {
            if (result.succeeded()) {
                LOG.info("AGUI Example Server started on port 8080");
                LOG.info("Open http://localhost:8080 in your browser to access the chat interface");
            } else {
                LOG.error("Failed to start server", result.cause());
            }
        });
    }

    private void handleHealth(RoutingContext ctx) {
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("status", "ok")
                        .put("service", "agui-example-server")
                        .encode());
    }

    private void handleRunAgent(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                ctx.response()
                        .setStatusCode(400)
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject()
                                .put("error", "Request body is required")
                                .encode());
                return;
            }

            String message = body.getString("message", "");
            String sessionId = body.getString("sessionId", "default");
            String runId = body.getString("runId", UUID.randomUUID().toString());

            // Get or create session
            List<BaseMessage> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

            // Build messages list with history + current message
            List<BaseMessage> allBaseMessages = new ArrayList<>(history);

            UserMessage userBaseMessage = new UserMessage();
            userBaseMessage.setContent(message);
            allBaseMessages.add(userBaseMessage);

            // Set up streaming response headers
            ctx.response()
                    .putHeader("Content-Type", "text/event-stream")
                    .putHeader("Cache-Control", "no-cache")
                    .putHeader("Connection", "keep-alive")
                    .setChunked(true);

            // Store final assistant response
            StringBuilder assistantContent = new StringBuilder();

            RunAgentParameters params = RunAgentParameters.builder()
                    .runId(runId)
                    .messages(allBaseMessages)
                    .build();

            // Create subscriber that writes directly to the response
            AtomicBoolean responseEnded = new AtomicBoolean(false);
            AgentSubscriber subscriber = createStreamSubscriber(
                    ctx.response(), runId, responseEnded, sessionId, userBaseMessage, assistantContent, history);

            agent.runAgent(params, subscriber);

            // After agent finishes, store the conversation in history
            // We need to wait for the agent to complete, but we can't block here
            // So we'll track the assistant's response in the subscriber and save after completion
            // For simplicity, let's create a simple approach - save after response ends

        } catch (Exception e) {
            LOG.error("Error handling run agent", e);
            if (!ctx.response().ended()) {
                ctx.response()
                        .setStatusCode(500)
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject()
                                .put("error", e.getMessage())
                                .encode());
            }
        }
    }

    private AgentSubscriber createStreamSubscriber(
            io.vertx.core.http.HttpServerResponse response,
            String runId,
            AtomicBoolean responseEnded,
            String sessionId,
            BaseMessage userBaseMessage,
            StringBuilder assistantContent,
            List<BaseMessage> history) {

        return new AgentSubscriber() {
            private void sendSSE(String eventType, Object data) {
                if (responseEnded.get() || response.ended()) {
                    return;
                }
                try {
                    String json = objectMapper.writeValueAsString(data);
                    response.write("event: " + eventType + "\n");
                    response.write("data: " + json + "\n\n");
                } catch (Exception e) {
                    LOG.error("Error sending event", e);
                }
            }

            @Override
            public void onEvent(BaseEvent event) {
                sendSSE("message", event);
            }

            @Override
            public void onTextMessageContentEvent(TextMessageContentEvent event) {
                LOG.info("[{}] Text: {}", runId, event.getDelta());
                assistantContent.append(event.getDelta());
                sendSSE("text", event);
            }

            @Override
            public void onToolCallStartEvent(ToolCallStartEvent event) {
                LOG.info("[{}] Tool call start: {}", runId, event.getToolCallName());
                sendSSE("toolCall", event);
            }

            @Override
            public void onToolCallResultEvent(ToolCallResultEvent event) {
                LOG.info("[{}] Tool result: {}", runId, event.getContent());
                sendSSE("toolResult", event);
            }

            @Override
            public void onRunFinishedEvent(RunFinishedEvent event) {
                LOG.info("[{}] Run finished", runId);

                // Save conversation to session history
                if (sessionId != null && history != null) {
                    history.add(userBaseMessage);
                    AssistantMessage assistantBaseMessage = new AssistantMessage();
                    assistantBaseMessage.setContent(assistantContent.toString());
                    history.add(assistantBaseMessage);
                    LOG.info("[{}] Saved {} messages to session {}", runId, history.size(), sessionId);
                }

                sendSSE("finished", event);
                if (!responseEnded.getAndSet(true) && !response.ended()) {
                    response.end();
                }
            }

            @Override
            public void onRunErrorEvent(RunErrorEvent event) {
                LOG.error("[{}] Run error: {}", runId, event.getError());
                sendSSE("error", event);
                if (!responseEnded.getAndSet(true) && !response.ended()) {
                    response.end();
                }
            }
        };
    }
}