package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.observability.TracingMiddleware;
import github.ponyhuang.agentframework.types.Message;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.util.List;

/**
 * Example demonstrating OpenTelemetry integration.
 */
public class ObservabilityExample {

    public static void main(String[] args) {
        // 1. Initialize OpenTelemetry (Logging Exporter)
        OpenTelemetry openTelemetry = initOpenTelemetry();
        Tracer tracer = openTelemetry.getTracer("com.microsoft.agentframework.samples");

        // 2. Create Agent with Tracing Middleware
        ChatClient client = ClientExample.openAIChatClient();
        
        Agent agent = AgentBuilder.builder()
                .name("ObservableAgent")
                .instructions("You are a helpful assistant.")
                .client(client)
                .middleware(new TracingMiddleware(tracer)) // Add tracing middleware
                .build();

        System.out.println("Starting observable agent run...");
        
        // 3. Run Agent
        // Spans will be logged to console by LoggingSpanExporter
        agent.run(List.of(Message.user("Hello, tell me a joke about observability.")));
        
        System.out.println("Agent run completed. Check logs for traces.");
    }

    private static OpenTelemetry initOpenTelemetry() {
        // Create a logging exporter that prints spans to the console
        LoggingSpanExporter exporter = LoggingSpanExporter.create();
        
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }
}
