package github.ponyhuang.agentframework.hooks;

/**
 * Observer interface for hook events.
 * Implementations can subscribe to specific events and handle them in the chain.
 */
public interface HookObserver {

    /**
     * Called when a subscribed event is published.
     *
     * @param event the event type
     * @param context the event context
     * @param chainContext the chain context for accumulating results
     * @return the hook result
     */
    HookResult onEvent(HookEvent event, HookContext context, ChainContext chainContext);

    /**
     * Returns the events this observer subscribes to.
     * Default implementation returns empty array, subclasses should override.
     *
     * @return array of subscribed events
     */
    default HookEvent[] getSubscribedEvents() {
        return new HookEvent[0];
    }

    /**
     * Returns the priority of this observer.
     * Lower values execute first in the chain.
     *
     * @return the priority (default 100)
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Returns the matcher for filtering when this observer should execute.
     * Null or "*" means always execute for matching events.
     *
     * @return the matcher pattern
     */
    default String getMatcher() {
        return null;
    }

    /**
     * Checks if this observer matches the given value.
     *
     * @param value the value to match against
     * @return true if matches, false otherwise
     */
    default boolean matches(String value) {
        String matcher = getMatcher();
        if (matcher == null || matcher.isEmpty() || "*".equals(matcher)) {
            return true;
        }
        if (value == null) {
            return false;
        }
        HookMatcher hookMatcher = new HookMatcher(matcher);
        return hookMatcher.matches(value);
    }
}