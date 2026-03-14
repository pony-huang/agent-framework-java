package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.providers.ChatClient;
import github.ponyhuang.agentframework.hooks.event.HookEventType;
import github.ponyhuang.agentframework.hooks.event.UserPromptSubmitEvent;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.TracingHookHandler;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.workflows.Workflow;
import github.ponyhuang.agentframework.workflows.WorkflowBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-End Example: Customer Support Bot
 *
 * Demonstrates:
 * 1. OpenTelemetry Tracing with Hooks
 * 2. Hooks for PII Redaction
 * 3. Context Provider for User Profile Injection
 * 4. Workflow for Intent Routing
 * 5. Memory for Session State
 */
public class EndToEndExample {

    public static void main(String[] args) {
        configureLogging();

        ChatClient client = ClientExample.openAIChatClient();

        OpenTelemetry openTelemetry = initOpenTelemetry();
        Tracer tracer = openTelemetry.getTracer("com.example.agentframework");
        TracingHookHandler handler = new TracingHookHandler(tracer);
        java.util.function.Function<github.ponyhuang.agentframework.hooks.event.BaseEvent, HookResult> userPromptSubmit = event -> {
            if (event instanceof UserPromptSubmitEvent promptEvent) {
                String text = promptEvent.getPrompt();
                if (text != null && text.contains("@")) {
                    text = text.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "[EMAIL_REDACTED]");
                    promptEvent.setPrompt(text);
                }
            }
            return HookResult.allow();
        };

        Agent triageAgent = AgentBuilder.builder()
                .name("TriageAgent")
                .instructions("Classify user intent into: REFUND, TECH_SUPPORT, or GENERAL. Reply ONLY with the category.")
                .client(client)
                .hook(HookEventType.USER_PROMPT_SUBMIT, userPromptSubmit)
                .hook(HookEventType.SESSION_START, handler)
                .hook(HookEventType.STOP, handler)
                .hook(HookEventType.PRE_TOOL_USE, handler)
                .hook(HookEventType.POST_TOOL_USE, handler)
                .build();

        Agent financeAgent = AgentBuilder.builder()
                .name("FinanceAgent")
                .instructions("You are a finance specialist. Help with refunds.")
                .client(client)
                .hook(HookEventType.USER_PROMPT_SUBMIT, userPromptSubmit)
                .hook(HookEventType.SESSION_START, handler)
                .hook(HookEventType.STOP, handler)
                .hook(HookEventType.PRE_TOOL_USE, handler)
                .hook(HookEventType.POST_TOOL_USE, handler)
                .build();

        Agent techAgent = AgentBuilder.builder()
                .name("TechAgent")
                .instructions("You are a technical support engineer.")
                .client(client)
                .hook(HookEventType.USER_PROMPT_SUBMIT, userPromptSubmit)
                .hook(HookEventType.SESSION_START, handler)
                .hook(HookEventType.STOP, handler)
                .hook(HookEventType.PRE_TOOL_USE, handler)
                .hook(HookEventType.POST_TOOL_USE, handler)
                .build();

        Agent generalAgent = AgentBuilder.builder()
                .name("GeneralAgent")
                .instructions("You are a general assistant.")
                .client(client)
                .hook(HookEventType.USER_PROMPT_SUBMIT, userPromptSubmit)
                .hook(HookEventType.SESSION_START, handler)
                .hook(HookEventType.STOP, handler)
                .hook(HookEventType.PRE_TOOL_USE, handler)
                .hook(HookEventType.POST_TOOL_USE, handler)
                .build();

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

                .addConditionalEdge("route", "refund", "$is_refund")
                .addConditionalEdge("route", "tech", "$is_tech")
                .addConditionalEdge("route", "general", "$is_general")

                .addEdge("refund", "end")
                .addEdge("tech", "end")
                .addEdge("general", "end")
                .build();

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
        } catch (Exception ignored) {
        }
    }
}
