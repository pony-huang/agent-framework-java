package github.ponyhuang.agentframework.hooks;

import github.ponyhuang.agentframework.hooks.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main hook execution engine.
 * Handles configuration loading, matcher filtering, and chain execution.
 *
 * @deprecated Use {@link HookEventBus} instead for observer + chain pattern.
 * This class is kept for backward compatibility.
 */
@Deprecated
public class HookExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(HookExecutor.class);

    private final HookEventBus eventBus;

    public HookExecutor() {
        this.eventBus = new HookEventBus();
    }

    public HookExecutor(Map<HookEvent, List<HookExecutor.HookRegistration>> hookConfigs,
                        ExecutorService executorService, boolean disabled) {
        this.eventBus = HookEventBus.builder()
                .executorService(executorService)
                .disabled(disabled)
                .build();

        // Migrate old registrations to new format
        if (hookConfigs != null) {
            for (Map.Entry<HookEvent, List<HookExecutor.HookRegistration>> entry : hookConfigs.entrySet()) {
                HookEvent event = entry.getKey();
                for (HookExecutor.HookRegistration reg : entry.getValue()) {
                    final String matcher = reg.getMatcher() != null ?
                            reg.getMatcher().getPatternString() : null;
                    eventBus.registerHook(event, reg.getHandler(), matcher);
                }
            }
        }
    }

    /**
     * Executes hooks for PreToolUse event.
     */
    public HookResult executePreToolUse(PreToolUseContext context) {
        return eventBus.executePreToolUse(context);
    }

    /**
     * Executes hooks for PostToolUse event.
     */
    public HookResult executePostToolUse(PostToolUseContext context) {
        return eventBus.executePostToolUse(context);
    }

    /**
     * Executes hooks for PostToolUseFailure event.
     */
    public HookResult executePostToolUseFailure(PostToolUseFailureContext context) {
        return eventBus.executePostToolUseFailure(context);
    }

    /**
     * Executes hooks for PermissionRequest event.
     */
    public HookResult executePermissionRequest(PermissionRequestContext context) {
        return eventBus.executePermissionRequest(context);
    }

    /**
     * Executes hooks for SessionStart event.
     */
    public HookResult executeSessionStart(SessionStartContext context) {
        return eventBus.executeSessionStart(context);
    }

    /**
     * Executes hooks for SessionEnd event.
     */
    public HookResult executeSessionEnd(SessionEndContext context) {
        return eventBus.executeSessionEnd(context);
    }

    /**
     * Executes hooks for Stop event.
     */
    public HookResult executeStop(StopContext context) {
        return eventBus.executeStop(context);
    }

    /**
     * Executes hooks for UserPromptSubmit event.
     */
    public HookResult executeUserPromptSubmit(UserPromptSubmitContext context) {
        return eventBus.executeUserPromptSubmit(context);
    }

    /**
     * Executes hooks for Notification event.
     */
    public HookResult executeNotification(NotificationContext context) {
        return eventBus.executeNotification(context);
    }

    /**
     * Executes hooks for SubagentStart event.
     */
    public HookResult executeSubagentStart(SubagentStartContext context) {
        return eventBus.executeSubagentStart(context);
    }

    /**
     * Executes hooks for SubagentStop event.
     */
    public HookResult executeSubagentStop(SubagentStopContext context) {
        return eventBus.executeSubagentStop(context);
    }

    /**
     * Executes hooks for TaskCompleted event.
     */
    public HookResult executeTaskCompleted(TaskCompletedContext context) {
        return eventBus.executeTaskCompleted(context);
    }

    /**
     * Executes hooks for ConfigChange event.
     */
    public HookResult executeConfigChange(ConfigChangeContext context) {
        return eventBus.executeConfigChange(context);
    }

    /**
     * Executes hooks for PreCompact event.
     */
    public HookResult executePreCompact(PreCompactContext context) {
        return eventBus.executePreCompact(context);
    }

    /**
     * Executes hooks for InstructionsLoaded event.
     */
    public HookResult executeInstructionsLoaded(InstructionsLoadedContext context) {
        return eventBus.executeInstructionsLoaded(context);
    }

    /**
     * Executes hooks for WorktreeCreate event.
     */
    public HookResult executeWorktreeCreate(WorktreeCreateContext context) {
        return eventBus.executeWorktreeCreate(context);
    }

    /**
     * Executes hooks for WorktreeRemove event.
     */
    public HookResult executeWorktreeRemove(WorktreeRemoveContext context) {
        return eventBus.executeWorktreeRemove(context);
    }

    /**
     * Executes hooks for TeammateIdle event.
     */
    public HookResult executeTeammateIdle(TeammateIdleContext context) {
        return eventBus.executeTeammateIdle(context);
    }

    /**
     * Registers a hook for an event.
     */
    public void registerHook(HookEvent event, HookHandler handler, String matcher) {
        eventBus.registerHook(event, handler, matcher);
    }

    /**
     * Registers a hook for an event without matcher.
     */
    public void registerHook(HookEvent event, HookHandler handler) {
        eventBus.registerHook(event, handler);
    }

    /**
     * Functional interface for simple hook handlers.
     */
    @FunctionalInterface
    public interface HookFunction {
        HookResult apply(HookContext context);
    }

    /**
     * Registers a hook with a simple function (lambda).
     */
    public void registerHook(HookEvent event, HookFunction function) {
        HookHandler handler = new HookHandler() {
            @Override
            public HookResult execute(HookContext context) {
                return function.apply(context);
            }

            @Override
            public HookHandlerType getType() {
                return HookHandlerType.COMMAND;
            }

            @Override
            public Duration getTimeout() {
                return Duration.ofSeconds(60);
            }
        };
        eventBus.registerHook(event, handler);
    }

    /**
     * Registers a hook with a simple function (lambda) and matcher.
     */
    public void registerHook(HookEvent event, HookFunction function, String matcher) {
        HookHandler handler = new HookHandler() {
            @Override
            public HookResult execute(HookContext context) {
                return function.apply(context);
            }

            @Override
            public HookHandlerType getType() {
                return HookHandlerType.COMMAND;
            }

            @Override
            public Duration getTimeout() {
                return Duration.ofSeconds(60);
            }
        };
        eventBus.registerHook(event, handler, matcher);
    }

    /**
     * Gets all registered hooks.
     * @deprecated Use {@link HookEventBus#getObservers()} instead.
     */
    @Deprecated
    public Map<HookEvent, List<HookRegistration>> getHookConfigs() {
        Map<HookEvent, List<HookRegistration>> result = new HashMap<>();
        Map<HookEvent, List<HookObserver>> observers = eventBus.getObservers();

        for (Map.Entry<HookEvent, List<HookObserver>> entry : observers.entrySet()) {
            List<HookRegistration> registrations = new ArrayList<>();
            for (HookObserver obs : entry.getValue()) {
                if (obs instanceof HookEventBus.HookObserverAdapter) {
                    HookEventBus.HookObserverAdapter adapter = (HookEventBus.HookObserverAdapter) obs;
                    registrations.add(new HookRegistration(
                            new HookMatcher(adapter.getMatcher()),
                            adapter.getHandler()
                    ));
                }
            }
            if (!registrations.isEmpty()) {
                result.put(entry.getKey(), registrations);
            }
        }
        return result;
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        eventBus.shutdown();
    }

    /**
     * Registration holding a matcher and handler.
     */
    public static class HookRegistration {
        private final HookMatcher matcher;
        private final HookHandler handler;

        public HookRegistration(HookMatcher matcher, HookHandler handler) {
            this.matcher = matcher;
            this.handler = handler;
        }

        public HookMatcher getMatcher() {
            return matcher;
        }

        public HookHandler getHandler() {
            return handler;
        }
    }

    /**
     * Builder for HookExecutor.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<HookEvent, List<HookRegistration>> hookConfigs = new HashMap<>();
        private ExecutorService executorService;
        private boolean disabled = false;

        public Builder hookConfigs(Map<HookEvent, List<HookRegistration>> hookConfigs) {
            this.hookConfigs = hookConfigs;
            return this;
        }

        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public HookExecutor build() {
            return new HookExecutor(hookConfigs, executorService, disabled);
        }
    }
}