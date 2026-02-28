package github.ponyhuang.agentframework.observability;

import github.ponyhuang.agentframework.middleware.AgentMiddleware;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.util.function.Function;

/**
 * Middleware that adds OpenTelemetry tracing to Agent execution.
 */
public class TracingMiddleware implements AgentMiddleware {

    private final Tracer tracer;

    public TracingMiddleware(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public ChatResponse process(AgentMiddlewareContext context, Function<AgentMiddlewareContext, ChatResponse> next) {
        String agentName = context.getAgent().getName();
        String spanName = "agent " + agentName;

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("gen_ai.system", "microsoft.agent_framework")
                .setAttribute("gen_ai.agent.name", agentName)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Add input attributes (simplified)
            if (!context.getMessages().isEmpty()) {
                Message lastMsg = context.getMessages().get(context.getMessages().size() - 1);
                span.setAttribute("gen_ai.last_message.role", lastMsg.getRole().toString());
            }

            ChatResponse response = next.apply(context);

            // Add response attributes
            if (response != null) {
                span.setAttribute("gen_ai.response.model", response.getModel());
                if (response.getUsage() != null) {
                    span.setAttribute("gen_ai.usage.prompt_tokens", (long) response.getUsage().getPromptTokens());
                    span.setAttribute("gen_ai.usage.completion_tokens", (long) response.getUsage().getCompletionTokens());
                    span.setAttribute("gen_ai.usage.total_tokens", (long) response.getUsage().getTotalTokens());
                }
            }
            
            span.setStatus(StatusCode.OK);
            return response;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
