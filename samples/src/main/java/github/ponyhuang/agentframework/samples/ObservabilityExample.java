package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.HookExecutor;
import github.ponyhuang.agentframework.observability.TracingHookHandler;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Example demonstrating OpenTelemetry integration with Hooks.
 */
public class ObservabilityExample {

    public static void main(String[] args) {
        OpenTelemetry openTelemetry = initOpenTelemetry();
        Tracer tracer = openTelemetry.getTracer("com.microsoft.agentframework.samples");

        ChatClient client = ClientExample.openAIChatClient();

        HookExecutor hookExecutor = HookExecutor.builder().build();
        TracingHookHandler.registerTracingHooks(hookExecutor, tracer);

        Agent agent = AgentBuilder.builder()
                .name("ObservableAgent")
                .instructions("You are a helpful assistant.")
                .client(client)
                .hookExecutor(hookExecutor)
                .build();

        System.out.println("Starting observable agent run...");

        agent.runStream(List.of(UserMessage.create("Hello, tell me a joke about observability."))).blockLast();

        System.out.println("Agent run completed. Check logs for traces.");
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
}
