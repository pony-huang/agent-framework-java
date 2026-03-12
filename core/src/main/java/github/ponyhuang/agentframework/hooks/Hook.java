package github.ponyhuang.agentframework.hooks;

import github.ponyhuang.agentframework.hooks.event.BaseEvent;
import github.ponyhuang.agentframework.hooks.event.HookEventType;

/**
 * Hook handler interface.
 * Implementations handle specific event types during agent execution.
 */
public interface Hook {

    /**
     * Handles the event.
     *
     * @param event the event to process
     * @return the hook result
     */
    HookResult onEvent(BaseEvent event);

    /**
     * Returns the event types this hook subscribes to.
     *
     * @return array of subscribed events
     */
    default HookEventType[] getSubscribedEvents() {
        return null;
    }

    /**
     * Returns the matcher pattern for filtering.
     *
     * @return matcher pattern, or null for all
     */
    default String getMatcher() {
        return null;
    }

    /**
     * Returns the priority of this hook.
     * Lower values execute first.
     *
     * @return priority (default 100)
     */
    default int getPriority() {
        return 100;
    }
}
