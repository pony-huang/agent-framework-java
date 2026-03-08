package github.ponyhuang.agentframework.observability;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;
import github.ponyhuang.agentframework.hooks.HookExecutor;
import github.ponyhuang.agentframework.hooks.HookHandler;
import github.ponyhuang.agentframework.hooks.HookHandlerType;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.events.PostToolUseContext;
import github.ponyhuang.agentframework.hooks.events.PreToolUseContext;
import github.ponyhuang.agentframework.hooks.events.SessionEndContext;
import github.ponyhuang.agentframework.hooks.events.SessionStartContext;
import github.ponyhuang.agentframework.hooks.events.StopContext;
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
    public HookResult execute(HookContext context) {
        if (tracer == null) {
            return HookResult.allow();
        }

        if (context instanceof SessionStartContext) {
            return handleSessionStart((SessionStartContext) context);
        } else if (context instanceof StopContext) {
            return handleStop((StopContext) context);
        } else if (context instanceof PreToolUseContext) {
            return handlePreToolUse((PreToolUseContext) context);
        } else if (context instanceof PostToolUseContext) {
            return handlePostToolUse((PostToolUseContext) context);
        }

        return HookResult.allow();
    }

    private HookResult handleSessionStart(SessionStartContext context) {
        String spanName = "agent " + (context.getAgentType() != null ? context.getAgentType() : "session");

        currentSpan = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("gen_ai.system", "microsoft.agent_framework")
                .setAttribute("gen_ai.agent.session_id", context.getSessionId() != null ? context.getSessionId() : UUID.randomUUID().toString())
                .setAttribute("gen_ai.agent.model", context.getModel() != null ? context.getModel() : "unknown")
                .startSpan();

        return HookResult.allow();
    }

    private HookResult handleStop(StopContext context) {
        if (currentSpan != null) {
            try (Scope scope = currentSpan.makeCurrent()) {
                if (context.getLastAssistantMessage() != null) {
                    currentSpan.setAttribute("gen_ai.response.message", context.getLastAssistantMessage());
                }
                currentSpan.setStatus(StatusCode.OK);
            } catch (Exception e) {
                currentSpan.recordException(e);
                currentSpan.setStatus(StatusCode.ERROR, e.getMessage());
            } finally {
                currentSpan.end();
                currentSpan = null;
            }
        }
        return HookResult.allow();
    }

    private HookResult handlePreToolUse(PreToolUseContext context) {
        if (tracer == null) {
            return HookResult.allow();
        }

        String spanName = "tool " + context.getToolName();
        Span toolSpan = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("gen_ai.system", "microsoft.agent_framework")
                .setAttribute("gen_ai.tool.name", context.getToolName())
                .setAttribute("gen_ai.tool.input", context.getToolInput() != null ? context.getToolInput().toString() : "")
                .startSpan();

        try (Scope scope = toolSpan.makeCurrent()) {
            return HookResult.allow();
        } catch (Exception e) {
            toolSpan.recordException(e);
            toolSpan.setStatus(StatusCode.ERROR, e.getMessage());
            toolSpan.end();
            return HookResult.allow();
        }
    }

    private HookResult handlePostToolUse(PostToolUseContext context) {
        return HookResult.allow();
    }

    @Override
    public HookHandlerType getType() {
        return HookHandlerType.COMMAND;
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofSeconds(60);
    }

    public static void registerTracingHooks(HookExecutor hookExecutor, Tracer tracer) {
        if (hookExecutor == null || tracer == null) {
            return;
        }

        TracingHookHandler handler = new TracingHookHandler(tracer);

        hookExecutor.registerHook(HookEvent.SESSION_START, handler);
        hookExecutor.registerHook(HookEvent.STOP, handler);
        hookExecutor.registerHook(HookEvent.PRE_TOOL_USE, handler);
        hookExecutor.registerHook(HookEvent.POST_TOOL_USE, handler);
    }
}
