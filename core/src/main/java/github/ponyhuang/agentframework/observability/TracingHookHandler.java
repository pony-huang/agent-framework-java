package github.ponyhuang.agentframework.observability;

import github.ponyhuang.agentframework.hooks.HookEventBus;
import github.ponyhuang.agentframework.hooks.HookHandler;
import github.ponyhuang.agentframework.hooks.HookHandlerType;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.event.BaseEvent;
import github.ponyhuang.agentframework.hooks.event.SessionEndEvent;
import github.ponyhuang.agentframework.hooks.event.SessionStartEvent;
import github.ponyhuang.agentframework.hooks.event.StopEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.time.Duration;
import java.util.UUID;

public class TracingHookHandler implements HookHandler {

    private final Tracer tracer;
    private Span currentSpan;

    public TracingHookHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public HookResult execute(BaseEvent event) {
        if (tracer == null) {
            return HookResult.allow();
        }

        if (event instanceof SessionStartEvent) {
            return handleSessionStart((SessionStartEvent) event);
        } else if (event instanceof StopEvent) {
            return handleStop((StopEvent) event);
        } else if (event instanceof SessionEndEvent) {
            return handleSessionEnd((SessionEndEvent) event);
        }

        return HookResult.allow();
    }

    private HookResult handleSessionStart(SessionStartEvent event) {
        currentSpan = tracer.spanBuilder("session")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("session.id", event.getSessionId())
                .setAttribute("source", event.getSource() != null ? event.getSource() : "unknown")
                .startSpan();

        return HookResult.allow();
    }

    private HookResult handleStop(StopEvent event) {
        if (currentSpan != null) {
            try (Scope scope = currentSpan.makeCurrent()) {
                currentSpan.setAttribute("message", event.getLastAssistantMessage() != null ? event.getLastAssistantMessage() : "");
                currentSpan.setStatus(StatusCode.OK);
            } finally {
                currentSpan.end();
            }
        }

        return HookResult.allow();
    }

    private HookResult handleSessionEnd(SessionEndEvent event) {
        if (currentSpan != null) {
            currentSpan.end();
        }

        return HookResult.allow();
    }

    @Override
    public HookHandlerType getType() {
        return HookHandlerType.PROMPT;
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofSeconds(60);
    }
}
