package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.middleware.AgentMiddleware;
import github.ponyhuang.agentframework.observability.TracingMiddleware;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.ContextProvider;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.message.SystemMessage;
import github.ponyhuang.agentframework.workflows.Workflow;
import github.ponyhuang.agentframework.workflows.WorkflowBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.UnsupportedEncodingException;

/**
 * End-to-End Example: Customer Support Bot
 *
 * Demonstrates:
 * 1. OpenTelemetry Tracing
 * 2. Middleware for PII Redaction
 * 3. Context Provider for User Profile Injection
 * 4. Workflow for Intent Routing
 * 5. Memory for Session State
 */
public class EndToEndExample {

    public static void main(String[] args) {
        // Fix console encoding for LoggingSpanExporter
        configureLogging();

        // Create a chat client
        ChatClient client = ClientExample.openAIChatClient();

        // 1. Setup Observability
        OpenTelemetry openTelemetry = initOpenTelemetry();
        Tracer tracer = openTelemetry.getTracer("com.example.agentframework");
        AgentMiddleware tracingMiddleware = new TracingMiddleware(tracer);

        // 2. Define Middleware (PII Redaction)
        AgentMiddleware piiMiddleware = (context, next) -> {
            // Simple redaction of emails
            List<Message> redactedMessages = new ArrayList<>();
            for (Message msg : context.getMessages()) {
                String text = msg.getTextContent();
                if (text != null && text.contains("@")) { // Naive email check
                    text = text.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "[EMAIL_REDACTED]");
                    redactedMessages.add("user".equalsIgnoreCase(msg.getRoleAsString()) ? UserMessage.create(text) : AssistantMessage.create(text));
                } else {
                    redactedMessages.add(msg);
                }
            }
            // In a real implementation, we would replace messages in context, but AgentMiddlewareContext.setMessages is not fully implemented in this sample
            // For now, we just proceed. Real implementation would modify the request.
            return next.apply(context);
        };

        // 3. Define Context Provider (User Profile)
        ContextProvider userProfileProvider = new ContextProvider() {
            @Override
            public List<Message> beforeRun(Object agent, AgentSession session, List<Message> messages, Map<String, Object> options) {
                // Read from options (which comes from Workflow Context) since we don't have a session in this workflow execution
                String userLevel = (String) options.get("user_level");
                if (userLevel == null) userLevel = "REGULAR";

                List<Message> newMessages = new ArrayList<>(messages);
                newMessages.add(0, SystemMessage.create("User Level: " + userLevel + ". Adjust tone accordingly."));
                return newMessages;
            }
        };

        // 4. Create Agents

        // Triage Agent: Analyzes intent
        Agent triageAgent = AgentBuilder.builder()
                .name("TriageAgent")
                .instructions("Classify user intent into: REFUND, TECH_SUPPORT, or GENERAL. Reply ONLY with the category.")
                .client(client)
                .build();

        triageAgent.addMiddleware((context, next) -> {
            ChatResponse response = next.apply(context);
            String intent = response.getMessage().getTextContent().trim().toUpperCase();

            Map<String, Object> extra = new HashMap<>();
            if (response.getExtraProperties() != null) extra.putAll(response.getExtraProperties());

            if (intent.contains("REFUND")) extra.put("is_refund", true);
            else if (intent.contains("TECH")) extra.put("is_tech", true);
            else extra.put("is_general", true);

            return ChatResponse.builder()
                    .id(response.getId())
                    .messages(response.getMessages())
                    .usage(response.getUsage())
                    .extraProperties(extra)
                    .build();
        });

        Agent financeAgent = AgentBuilder.builder()
                .name("FinanceAgent")
                .instructions("You are a finance specialist. Help with refunds.")
                .client(client)
                .middleware(tracingMiddleware)
                .contextProvider(userProfileProvider)
                .build();

        Agent techAgent = AgentBuilder.builder()
                .name("TechAgent")
                .instructions("You are a technical support engineer.")
                .client(client)
                .middleware(tracingMiddleware)
                .contextProvider(userProfileProvider)
                .build();

        Agent generalAgent = AgentBuilder.builder()
                .name("GeneralAgent")
                .instructions("You are a general assistant.")
                .client(client)
                .middleware(tracingMiddleware)
                .contextProvider(userProfileProvider)
                .build();

        // 5. Build Workflow
        Workflow workflow = WorkflowBuilder.builder()
                .name("SupportWorkflow")
                .addAgentNode("triage", triageAgent)
                .addConditionNode("route", "Route", c -> true)
                .addAgentNode("refund", financeAgent)
                .addAgentNode("tech", techAgent)
                .addAgentNode("general", generalAgent)
                .addEndNode("end", "End")

                .startAt("triage")
                .addEdge("triage", "route")

                // Intent routing based on triage
                .addConditionalEdge("route", "refund", "$is_refund")
                .addConditionalEdge("route", "tech", "$is_tech")
                .addConditionalEdge("route", "general", "$is_general")

                .addEdge("refund", "end")
                .addEdge("tech", "end")
                .addEdge("general", "end")
                .build();

        // 6. Execute Workflow
        Map<String, Object> context = new HashMap<>();
        context.put("user_level", "VIP");

        String input = "Hi, I need help with my order #12345. Also, my email is john.doe@example.com";
        context.put("messages", List.of(UserMessage.create(input)));

        Workflow.Result result = workflow.execute(context);

        System.out.println("\n--- Workflow Result ---");
        System.out.println("Success: " + result.isSuccess());
        if (result.getMessages() != null) {
            System.out.println("Final Response: " + result.getMessages().get(result.getMessages().size() - 1).getTextContent());
        }
    }

    private static OpenTelemetry initOpenTelemetry() {
        LoggingSpanExporter exporter = LoggingSpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }

    private static void configureLogging() {
        try {
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("sun.stdout.encoding", "UTF-8");
            System.setProperty("sun.stderr.encoding", "UTF-8");
        } catch (Exception e) {
            // Ignore
        }
    }
}
