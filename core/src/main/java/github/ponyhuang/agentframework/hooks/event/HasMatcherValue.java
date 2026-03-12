package github.ponyhuang.agentframework.hooks.event;

/**
 * Interface for events that have a matcher value.
 * Used by HookEventBus to filter which hooks should handle the event.
 */
public interface HasMatcherValue {

    /**
     * Returns the value to use for matcher filtering.
     *
     * @return the matcher value, or null if no filtering needed
     */
    String getMatcherValue();
}
