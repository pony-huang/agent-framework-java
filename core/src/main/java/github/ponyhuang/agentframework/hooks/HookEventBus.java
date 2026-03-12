package github.ponyhuang.agentframework.hooks;

import github.ponyhuang.agentframework.hooks.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Event bus for hook management and execution.
 * Implements observer pattern for event subscription and chain pattern for execution.
 */
public class HookEventBus {

    private static final Logger LOG = LoggerFactory.getLogger(HookEventBus.class);

    private final Map<HookEventType, List<HookObserver>> observers;
    private final ExecutorService executorService;
    private final boolean disabled;

    public HookEventBus() {
        this(new HashMap<>(), null, false);
    }

    public HookEventBus(Map<HookEventType, List<HookObserver>> observers,
                        ExecutorService executorService, boolean disabled) {
        this.observers = observers != null ? observers : new HashMap<>();
        this.executorService = executorService != null ? executorService : Executors.newCachedThreadPool();
        this.disabled = disabled;
    }

    /**
     * Subscribes an observer to an event.
     */
    public void subscribe(HookEventType event, HookObserver observer) {
        observers.computeIfAbsent(event, k -> new ArrayList<>()).add(observer);
        LOG.debug("Subscribed {} to event {}", observer.getClass().getSimpleName(), event);
    }

    /**
     * Unsubscribes an observer from an event.
     */
    public void unsubscribe(HookEventType event, HookObserver observer) {
        List<HookObserver> list = observers.get(event);
        if (list != null) {
            list.remove(observer);
            LOG.debug("Unsubscribed {} from event {}", observer.getClass().getSimpleName(), event);
        }
    }

    /**
     * Publishes a BaseEvent, triggering the observer chain.
     */
    public HookResult publish(BaseEvent event) {
        if (disabled) {
            LOG.debug("Hooks disabled, skipping {}", event.getType());
            return HookResult.allow();
        }

        HookEventType eventType = event.getType();
        List<HookObserver> registered = observers.get(eventType);

        if (registered == null || registered.isEmpty()) {
            return HookResult.allow();
        }

        // Get matcher value from event if available
        String matcherValue = null;
        if (event instanceof HasMatcherValue) {
            matcherValue = ((HasMatcherValue) event).getMatcherValue();
        }

        // Filter by matcher and sort by priority
        List<HookObserver> matchingObservers = new ArrayList<>();
        for (HookObserver obs : registered) {
            if (obs.matches(matcherValue)) {
                matchingObservers.add(obs);
            }
        }
        matchingObservers.sort(Comparator.comparingInt(HookObserver::getPriority));

        if (matchingObservers.isEmpty()) {
            return HookResult.allow();
        }

        LOG.debug("Executing chain with {} observers for {}", matchingObservers.size(), eventType);

        // Create chain context
        ChainContext chainContext = new ChainContext(matcherValue);

        // Execute chain sequentially
        for (HookObserver observer : matchingObservers) {
            if (!chainContext.shouldContinue()) {
                LOG.debug("Chain stopped, skipping remaining observers");
                break;
            }

            try {
                HookResult result = observer.onEvent(event, chainContext);
                chainContext.accumulate(result);

                LOG.debug("Observer {} returned: allow={}",
                    observer.getClass().getSimpleName(), result.isAllow());

            } catch (Exception e) {
                LOG.error("Observer execution error: {}", e.getMessage());
                chainContext.accumulate(HookResult.deny("Hook execution error: " + e.getMessage()));
            }
        }

        return chainContext.getAccumulatedResult();
    }

    // Convenience methods for new events

    public HookResult executePreToolUse(PreToolUseEvent event) {
        return publish(event);
    }

    public HookResult executePostToolUse(PostToolUseEvent event) {
        return publish(event);
    }

    public HookResult executePostToolUseFailure(PostToolUseFailureEvent event) {
        return publish(event);
    }

    public HookResult executePermissionRequest(PermissionRequestEvent event) {
        return publish(event);
    }

    public HookResult executeSessionStart(SessionStartEvent event) {
        return publish(event);
    }

    public HookResult executeSessionEnd(SessionEndEvent event) {
        return publish(event);
    }

    public HookResult executeStop(StopEvent event) {
        return publish(event);
    }

    public HookResult executeUserPromptSubmit(UserPromptSubmitEvent event) {
        return publish(event);
    }

    public HookResult executeNotification(NotificationEvent event) {
        return publish(event);
    }

    public HookResult executeSubagentStart(SubagentStartEvent event) {
        return publish(event);
    }

    public HookResult executeSubagentStop(SubagentStopEvent event) {
        return publish(event);
    }

    public HookResult executeTaskCompleted(TaskCompletedEvent event) {
        return publish(event);
    }

    public HookResult executeConfigChange(ConfigChangeEvent event) {
        return publish(event);
    }

    public HookResult executePreCompact(PreCompactEvent event) {
        return publish(event);
    }

    public HookResult executeInstructionsLoaded(InstructionsLoadedEvent event) {
        return publish(event);
    }

    public HookResult executeWorktreeCreate(WorktreeCreateEvent event) {
        return publish(event);
    }

    public HookResult executeWorktreeRemove(WorktreeRemoveEvent event) {
        return publish(event);
    }

    public HookResult executeTeammateIdle(TeammateIdleEvent event) {
        return publish(event);
    }

    // ===== New Hook Interface Support =====

    /**
     * Registers a Hook to handle specified events.
     */
    public void registerHook(Hook hook) {
        HookEventType[] events = hook.getSubscribedEvents();
        if (events != null) {
            for (HookEventType event : events) {
                HookObserverAdapter adapter = new HookObserverAdapter(hook);
                subscribe(event, adapter);
            }
        }
    }

    /**
     * Registers a Hook for a specific event type.
     */
    public void registerHook(HookEventType eventType, Hook hook) {
        HookObserverAdapter adapter = new HookObserverAdapter(hook);
        subscribe(eventType, adapter);
    }

    /**
     * Registers a Hook for a specific event type with matcher.
     */
    public void registerHook(HookEventType eventType, Hook hook, String matcher) {
        HookObserverAdapter adapter = new HookObserverAdapter(hook, matcher);
        subscribe(eventType, adapter);
    }

    /**
     * Registers a HookHandler for a specific event type.
     */
    public void registerHook(HookEventType eventType, HookHandler handler) {
        registerHook(eventType, handler, null);
    }

    /**
     * Registers a HookHandler for a specific event type with matcher.
     */
    public void registerHook(HookEventType eventType, HookHandler handler, String matcher) {
        Hook handlerAdapter = new Hook() {
            @Override
            public HookResult onEvent(BaseEvent event) {
                return handler.execute(event);
            }

            @Override
            public HookEventType[] getSubscribedEvents() {
                return new HookEventType[]{ eventType };
            }

            @Override
            public String getMatcher() {
                return matcher;
            }
        };
        registerHook(eventType, handlerAdapter);
    }

    /**
     * Registers a hook with a lambda function.
     */
    public void registerHook(HookEventType eventType, Function<BaseEvent, HookResult> function) {
        registerHook(eventType, function, null);
    }

    /**
     * Registers a hook with a lambda function and matcher.
     */
    public void registerHook(HookEventType eventType, Function<BaseEvent, HookResult> function, String matcher) {
        Hook adapter = new Hook() {
            @Override
            public HookResult onEvent(BaseEvent event) {
                return function.apply(event);
            }

            @Override
            public HookEventType[] getSubscribedEvents() {
                return new HookEventType[]{ eventType };
            }

            @Override
            public String getMatcher() {
                return matcher;
            }
        };
        registerHook(eventType, adapter);
    }

    /**
     * Gets all registered observers.
     */
    public Map<HookEventType, List<HookObserver>> getObservers() {
        return Collections.unmodifiableMap(observers);
    }

    /**
     * Gets observers for a specific event.
     */
    public List<HookObserver> getObservers(HookEventType eventType) {
        List<HookObserver> list = observers.get(eventType);
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

    /**
     * Adapter to convert Hook to HookObserver.
     */
    static class HookObserverAdapter implements HookObserver {
        private final Hook hook;
        private final String matcher;

        HookObserverAdapter(Hook hook) {
            this(hook, null);
        }

        HookObserverAdapter(Hook hook, String matcher) {
            this.hook = hook;
            this.matcher = matcher;
        }

        @Override
        public HookResult onEvent(BaseEvent event, ChainContext chainContext) {
            return hook.onEvent(event);
        }

        @Override
        public HookEventType[] getSubscribedEvents() {
            return hook.getSubscribedEvents();
        }

        @Override
        public String getMatcher() {
            return matcher != null ? matcher : hook.getMatcher();
        }

        @Override
        public int getPriority() {
            return hook.getPriority();
        }
    }

    /**
     * Builder for HookEventBus.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<HookEventType, List<HookObserver>> observers = new HashMap<>();
        private ExecutorService executorService;
        private boolean disabled = false;

        public Builder observers(Map<HookEventType, List<HookObserver>> observers) {
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
