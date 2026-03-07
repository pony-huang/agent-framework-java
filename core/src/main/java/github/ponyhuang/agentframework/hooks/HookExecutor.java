package github.ponyhuang.agentframework.hooks;

import github.ponyhuang.agentframework.hooks.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Main hook execution engine.
 * Handles configuration loading, matcher filtering, and parallel execution.
 */
public class HookExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(HookExecutor.class);

    private final Map<HookEvent, List<HookRegistration>> hookConfigs;
    private final ExecutorService executorService;
    private final boolean disabled;

    public HookExecutor() {
        this(new HashMap<>(), null, false);
    }

    public HookExecutor(Map<HookEvent, List<HookRegistration>> hookConfigs,
                        ExecutorService executorService, boolean disabled) {
        this.hookConfigs = hookConfigs != null ? hookConfigs : new HashMap<>();
        this.executorService = executorService != null ? executorService : Executors.newCachedThreadPool();
        this.disabled = disabled;
    }

    /**
     * Executes hooks for PreToolUse event.
     *
     * @param context the pre-tool-use context
     * @return the hook result
     */
    public HookResult executePreToolUse(PreToolUseContext context) {
        return executeHooks(context, context.getToolName());
    }

    /**
     * Executes hooks for PostToolUse event.
     *
     * @param context the post-tool-use context
     * @return the hook result
     */
    public HookResult executePostToolUse(PostToolUseContext context) {
        return executeHooks(context, context.getToolName());
    }

    /**
     * Executes hooks for PostToolUseFailure event.
     *
     * @param context the post-tool-use-failure context
     * @return the hook result
     */
    public HookResult executePostToolUseFailure(PostToolUseFailureContext context) {
        return executeHooks(context, context.getToolName());
    }

    /**
     * Executes hooks for PermissionRequest event.
     *
     * @param context the permission request context
     * @return the hook result
     */
    public HookResult executePermissionRequest(PermissionRequestContext context) {
        return executeHooks(context, context.getToolName());
    }

    /**
     * Executes hooks for SessionStart event.
     *
     * @param context the session start context
     * @return the hook result
     */
    public HookResult executeSessionStart(SessionStartContext context) {
        return executeHooks(context, context.getSource());
    }

    /**
     * Executes hooks for SessionEnd event.
     *
     * @param context the session end context
     * @return the hook result
     */
    public HookResult executeSessionEnd(SessionEndContext context) {
        return executeHooks(context, context.getReason());
    }

    /**
     * Executes hooks for Stop event.
     *
     * @param context the stop context
     * @return the hook result
     */
    public HookResult executeStop(StopContext context) {
        return executeHooks(context, null);
    }

    /**
     * Executes hooks for UserPromptSubmit event.
     *
     * @param context the user prompt submit context
     * @return the hook result
     */
    public HookResult executeUserPromptSubmit(UserPromptSubmitContext context) {
        return executeHooks(context, null);
    }

    /**
     * Executes hooks for Notification event.
     *
     * @param context the notification context
     * @return the hook result
     */
    public HookResult executeNotification(NotificationContext context) {
        return executeHooks(context, context.getNotificationType());
    }

    /**
     * Executes hooks for SubagentStart event.
     *
     * @param context the subagent start context
     * @return the hook result
     */
    public HookResult executeSubagentStart(SubagentStartContext context) {
        return executeHooks(context, context.getAgentType());
    }

    /**
     * Executes hooks for SubagentStop event.
     *
     * @param context the subagent stop context
     * @return the hook result
     */
    public HookResult executeSubagentStop(SubagentStopContext context) {
        return executeHooks(context, context.getAgentType());
    }

    /**
     * Executes hooks for TaskCompleted event.
     *
     * @param context the task completed context
     * @return the hook result
     */
    public HookResult executeTaskCompleted(TaskCompletedContext context) {
        return executeHooks(context, null);
    }

    /**
     * Executes hooks for ConfigChange event.
     *
     * @param context the config change context
     * @return the hook result
     */
    public HookResult executeConfigChange(ConfigChangeContext context) {
        return executeHooks(context, context.getSource());
    }

    /**
     * Executes hooks for PreCompact event.
     *
     * @param context the pre-compact context
     * @return the hook result
     */
    public HookResult executePreCompact(PreCompactContext context) {
        return executeHooks(context, context.getTrigger());
    }

    /**
     * Executes hooks for InstructionsLoaded event.
     *
     * @param context the instructions loaded context
     * @return the hook result
     */
    public HookResult executeInstructionsLoaded(InstructionsLoadedContext context) {
        return executeHooks(context, null);
    }

    /**
     * Executes hooks for WorktreeCreate event.
     *
     * @param context the worktree create context
     * @return the hook result
     */
    public HookResult executeWorktreeCreate(WorktreeCreateContext context) {
        return executeHooks(context, null);
    }

    /**
     * Executes hooks for WorktreeRemove event.
     *
     * @param context the worktree remove context
     * @return the hook result
     */
    public HookResult executeWorktreeRemove(WorktreeRemoveContext context) {
        return executeHooks(context, null);
    }

    /**
     * Executes hooks for TeammateIdle event.
     *
     * @param context the teammate idle context
     * @return the hook result
     */
    public HookResult executeTeammateIdle(TeammateIdleContext context) {
        return executeHooks(context, null);
    }

    /**
     * Core method to execute hooks for a given context.
     * The event type is derived from the context itself.
     */
    private HookResult executeHooks(HookContext context, String matcherValue) {
        HookEvent event = context.getHookEventName();

        if (disabled) {
            LOG.debug("Hooks disabled, skipping {}", event);
            return HookResult.allow();
        }

        List<HookRegistration> registrations = hookConfigs.get(event);
        if (registrations == null || registrations.isEmpty()) {
            return HookResult.allow();
        }

        // Filter by matcher
        List<HookRegistration> matchingHooks = registrations.stream()
                .filter(reg -> reg.matcher == null || reg.matcher.matches(matcherValue))
                .toList();

        if (matchingHooks.isEmpty()) {
            return HookResult.allow();
        }

        LOG.debug("Executing {} hooks for {}", matchingHooks.size(), event);

        // Execute matching hooks in parallel
        List<Future<HookResult>> futures = matchingHooks.stream()
                .map(reg -> executorService.submit(() -> executeHook(reg, context)))
                .toList();

        // Collect results
        HookResult aggregatedResult = HookResult.allow();
        StringBuilder additionalContext = new StringBuilder();

        for (Future<HookResult> future : futures) {
            try {
                HookResult result = future.get(60, TimeUnit.SECONDS);
                if (!result.isAllow()) {
                    // Block/deny takes precedence
                    aggregatedResult.setAllow(false);
                    if (result.getReason() != null) {
                        aggregatedResult.setReason(result.getReason());
                    }
                }

                // Aggregate additional context
                if (result.getAdditionalContext() != null) {
                    additionalContext.append(result.getAdditionalContext()).append("\n");
                }

                // Apply hook-specific output
                if (result.getHookSpecificOutput() != null) {
                    aggregatedResult.setHookSpecificOutput(result.getHookSpecificOutput());
                }

                // Check stopReason
                if (!result.isShouldContinue()) {
                    aggregatedResult.setShouldContinue(false);
                    if (result.getStopReason() != null) {
                        aggregatedResult.setStopReason(result.getStopReason());
                    }
                }
            } catch (Exception e) {
                LOG.error("Hook execution failed: {}", e.getMessage());
            }
        }

        if (!additionalContext.isEmpty()) {
            aggregatedResult.setAdditionalContext(additionalContext.toString().trim());
        }

        return aggregatedResult;
    }

    private HookResult executeHook(HookRegistration reg, HookContext context) {
        try {
            LOG.debug("Executing hook: {}", reg.handler.getType());
            return reg.handler.execute(context);
        } catch (Exception e) {
            LOG.error("Hook execution error: {}", e.getMessage());
            return HookResult.allow();
        }
    }

    /**
     * Registers a hook for an event.
     */
    public void registerHook(HookEvent event, HookHandler handler, String matcher) {
        hookConfigs.computeIfAbsent(event, k -> new ArrayList<>())
                .add(new HookRegistration(new HookMatcher(matcher), handler));
    }

    /**
     * Registers a hook for an event without matcher.
     */
    public void registerHook(HookEvent event, HookHandler handler) {
        registerHook(event, handler, null);
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
     *
     * @param event the hook event
     * @param function the hook function
     */
    public void registerHook(HookEvent event, HookFunction function) {
        registerHook(event, function, null);
    }

    /**
     * Registers a hook with a simple function (lambda) and matcher.
     *
     * @param event the hook event
     * @param function the hook function
     * @param matcher the matcher pattern
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
        registerHook(event, handler, matcher);
    }

    /**
     * Gets all registered hooks.
     */
    public Map<HookEvent, List<HookRegistration>> getHookConfigs() {
        return Collections.unmodifiableMap(hookConfigs);
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
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
