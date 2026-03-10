package github.ponyhuang.agentframework.hooks;

import github.ponyhuang.agentframework.hooks.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Event bus for hook management and execution.
 * Implements observer pattern for event subscription and chain pattern for execution.
 */
public class HookEventBus {

    private static final Logger LOG = LoggerFactory.getLogger(HookEventBus.class);

    private final Map<HookEvent, List<HookObserver>> observers;
    private final ExecutorService executorService;
    private final boolean disabled;

    public HookEventBus() {
        this(new HashMap<>(), null, false);
    }

    public HookEventBus(Map<HookEvent, List<HookObserver>> observers,
                        ExecutorService executorService, boolean disabled) {
        this.observers = observers != null ? observers : new HashMap<>();
        this.executorService = executorService != null ? executorService : Executors.newCachedThreadPool();
        this.disabled = disabled;
    }

    /**
     * Subscribes an observer to an event.
     *
     * @param event the event to subscribe to
     * @param observer the observer to add
     */
    public void subscribe(HookEvent event, HookObserver observer) {
        observers.computeIfAbsent(event, k -> new ArrayList<>()).add(observer);
        LOG.debug("Subscribed {} to event {}", observer.getClass().getSimpleName(), event);
    }

    /**
     * Unsubscribes an observer from an event.
     *
     * @param event the event to unsubscribe from
     * @param observer the observer to remove
     */
    public void unsubscribe(HookEvent event, HookObserver observer) {
        List<HookObserver> list = observers.get(event);
        if (list != null) {
            list.remove(observer);
            LOG.debug("Unsubscribed {} from event {}", observer.getClass().getSimpleName(), event);
        }
    }

    /**
     * Publishes an event, triggering the observer chain.
     *
     * @param event the event to publish
     * @param context the event context
     * @return the accumulated result from the chain
     */
    public HookResult publish(HookEvent event, HookContext context) {
        return publish(event, context, null);
    }

    /**
     * Publishes an event with a matcher value for filtering.
     *
     * @param event the event to publish
     * @param context the event context
     * @param matcherValue the value to match against
     * @return the accumulated result from the chain
     */
    public HookResult publish(HookEvent event, HookContext context, String matcherValue) {
        if (disabled) {
            LOG.debug("Hooks disabled, skipping {}", event);
            return HookResult.allow();
        }

        List<HookObserver> registered = observers.get(event);
        if (registered == null || registered.isEmpty()) {
            return HookResult.allow();
        }

        // Filter by matcher and sort by priority
        List<HookObserver> matchingObservers = registered.stream()
                .filter(obs -> obs.matches(matcherValue))
                .sorted(Comparator.comparingInt(HookObserver::getPriority))
                .collect(Collectors.toList());

        if (matchingObservers.isEmpty()) {
            return HookResult.allow();
        }

        LOG.debug("Executing chain with {} observers for {}", matchingObservers.size(), event);

        // Create chain context
        ChainContext chainContext = new ChainContext(matcherValue);

        // Execute chain sequentially
        for (HookObserver observer : matchingObservers) {
            if (!chainContext.shouldContinue()) {
                LOG.debug("Chain stopped, skipping remaining observers");
                break;
            }

            try {
                HookResult result = observer.onEvent(event, context, chainContext);
                chainContext.accumulate(result);
                chainContext.next();

                LOG.debug("Observer {} returned: allow={}",
                    observer.getClass().getSimpleName(), result.isAllow());

            } catch (Exception e) {
                LOG.error("Observer execution error: {}", e.getMessage());
                // Continue chain on error, but mark as not allow
                chainContext.accumulate(HookResult.deny("Hook execution error: " + e.getMessage()));
            }
        }

        return chainContext.getAccumulatedResult();
    }

    // Convenience methods for backward compatibility

    /**
     * Registers a hook for an event (backward compatible method).
     */
    public void registerHook(HookEvent event, HookHandler handler, String matcher) {
        HookObserverAdapter adapter = new HookObserverAdapter(handler, event, matcher);
        subscribe(event, adapter);
    }

    /**
     * Registers a hook for an event without matcher (backward compatible method).
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
     * Gets all registered observers.
     */
    public Map<HookEvent, List<HookObserver>> getObservers() {
        return Collections.unmodifiableMap(observers);
    }

    /**
     * Gets observers for a specific event.
     */
    public List<HookObserver> getObservers(HookEvent event) {
        List<HookObserver> list = observers.get(event);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    // Backward compatible execute methods (delegating to publish)

    public HookResult executePreToolUse(PreToolUseContext context) {
        return publish(HookEvent.PRE_TOOL_USE, context, context.getToolName());
    }

    public HookResult executePostToolUse(PostToolUseContext context) {
        return publish(HookEvent.POST_TOOL_USE, context, context.getToolName());
    }

    public HookResult executePostToolUseFailure(PostToolUseFailureContext context) {
        return publish(HookEvent.POST_TOOL_USE_FAILURE, context, context.getToolName());
    }

    public HookResult executePermissionRequest(PermissionRequestContext context) {
        return publish(HookEvent.PERMISSION_REQUEST, context, context.getToolName());
    }

    public HookResult executeSessionStart(SessionStartContext context) {
        return publish(HookEvent.SESSION_START, context, context.getSource());
    }

    public HookResult executeSessionEnd(SessionEndContext context) {
        return publish(HookEvent.SESSION_END, context, context.getReason());
    }

    public HookResult executeStop(StopContext context) {
        return publish(HookEvent.STOP, context, null);
    }

    public HookResult executeUserPromptSubmit(UserPromptSubmitContext context) {
        return publish(HookEvent.USER_PROMPT_SUBMIT, context, null);
    }

    public HookResult executeNotification(NotificationContext context) {
        return publish(HookEvent.NOTIFICATION, context, context.getNotificationType());
    }

    public HookResult executeSubagentStart(SubagentStartContext context) {
        return publish(HookEvent.SUBAGENT_START, context, context.getAgentType());
    }

    public HookResult executeSubagentStop(SubagentStopContext context) {
        return publish(HookEvent.SUBAGENT_STOP, context, context.getAgentType());
    }

    public HookResult executeTaskCompleted(TaskCompletedContext context) {
        return publish(HookEvent.TASK_COMPLETED, context, null);
    }

    public HookResult executeConfigChange(ConfigChangeContext context) {
        return publish(HookEvent.CONFIG_CHANGE, context, context.getSource());
    }

    public HookResult executePreCompact(PreCompactContext context) {
        return publish(HookEvent.PRE_COMPACT, context, context.getTrigger());
    }

    public HookResult executeInstructionsLoaded(InstructionsLoadedContext context) {
        return publish(HookEvent.INSTRUCTIONS_LOADED, context, null);
    }

    public HookResult executeWorktreeCreate(WorktreeCreateContext context) {
        return publish(HookEvent.WORKTREE_CREATE, context, null);
    }

    public HookResult executeWorktreeRemove(WorktreeRemoveContext context) {
        return publish(HookEvent.WORKTREE_REMOVE, context, null);
    }

    public HookResult executeTeammateIdle(TeammateIdleContext context) {
        return publish(HookEvent.TEAMMATE_IDLE, context, null);
    }

    /**
     * Adapter to convert HookHandler to HookObserver.
     */
    static class HookObserverAdapter implements HookObserver {
        private final HookHandler handler;
        private final HookEvent[] events;
        private final String matcher;

        HookObserverAdapter(HookHandler handler, HookEvent event, String matcher) {
            this.handler = handler;
            this.events = new HookEvent[]{event};
            this.matcher = matcher;
        }

        /**
         * Gets the underlying handler.
         */
        public HookHandler getHandler() {
            return handler;
        }

        @Override
        public HookResult onEvent(HookEvent event, HookContext context, ChainContext chainContext) {
            return handler.execute(context);
        }

        @Override
        public HookEvent[] getSubscribedEvents() {
            return events;
        }

        @Override
        public String getMatcher() {
            return matcher;
        }

        @Override
        public int getPriority() {
            return 100;
        }
    }

    /**
     * Builder for HookEventBus.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<HookEvent, List<HookObserver>> observers = new HashMap<>();
        private ExecutorService executorService;
        private boolean disabled = false;

        public Builder observers(Map<HookEvent, List<HookObserver>> observers) {
            this.observers = observers;
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

        public HookEventBus build() {
            return new HookEventBus(observers, executorService, disabled);
        }
    }
}