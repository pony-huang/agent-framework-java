package github.ponyhuang.agentframework.agui.example;

import com.agui.core.agent.Agent;
import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.*;
import com.agui.core.message.UserMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.ponyhuang.agentframework.agui.agent.AguiAgent;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.providers.OpenAIChatClient;
import github.ponyhuang.agentframework.tools.FunctionTool;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AguiVertxServer extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(AguiVertxServer.class);

    private final Agent agent;
    private final ObjectMapper objectMapper;
    private final Map<String, JsonObject> sessions = new ConcurrentHashMap<>();

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

        router.get("/health").handler(this::handleHealth);
        router.post("/agent/run").handler(this::handleRunAgent);
        router.get("/agent/events/:runId").handler(this::handleEventStream);

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
            String message = body.getString("message", "");
            String runId = body.getString("runId", UUID.randomUUID().toString());

            sessions.put(runId, new JsonObject().put("status", "running"));

            UserMessage userMessage = new UserMessage();
            userMessage.setContent(message);

            RunAgentParameters params = RunAgentParameters.builder()
                    .runId(runId)
                    .messages(List.of(userMessage))
                    .build();

            AgentSubscriber subscriber = createJsonSubscriber(runId, body);

            agent.runAgent(params, subscriber);

            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("runId", runId)
                            .put("status", "started")
                            .encode());

        } catch (Exception e) {
            LOG.error("Error handling run agent", e);
            ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("error", e.getMessage())
                            .encode());
        }
    }

    private void handleEventStream(RoutingContext ctx) {
        String runId = ctx.pathParam("runId");
        ctx.response()
                .putHeader("Content-Type", "text/event-stream")
                .putHeader("Cache-Control", "no-cache")
                .putHeader("Connection", "keep-alive")
                .setChunked(true);

        JsonObject session = sessions.get(runId);
        if (session != null) {
            ctx.response().end();
        } else {
            ctx.response().end();
        }
    }

    private AgentSubscriber createJsonSubscriber(String runId, JsonObject request) {
        return new AgentSubscriber() {
            @Override
            public void onEvent(BaseEvent event) {
                try {
                    String json = objectMapper.writeValueAsString(event);
                    LOG.debug("Event: {}", json);
                } catch (Exception e) {
                    LOG.error("Error serializing event", e);
                }
            }

            @Override
            public void onTextMessageContentEvent(TextMessageContentEvent event) {
                LOG.info("[{}] Text: {}", runId, event.getDelta());
            }

            @Override
            public void onToolCallStartEvent(ToolCallStartEvent event) {
                LOG.info("[{}] Tool call start: {}", runId, event.getToolCallName());
            }

            @Override
            public void onToolCallResultEvent(ToolCallResultEvent event) {
                LOG.info("[{}] Tool result: {}", runId, event.getContent());
            }

            @Override
            public void onRunFinishedEvent(RunFinishedEvent event) {
                LOG.info("[{}] Run finished", runId);
                sessions.remove(runId);
            }

            @Override
            public void onRunErrorEvent(RunErrorEvent event) {
                LOG.error("[{}] Run error: {}", runId, event.getError());
            }
        };
    }
}
