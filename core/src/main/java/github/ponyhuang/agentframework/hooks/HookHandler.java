package github.ponyhuang.agentframework.hooks;


import java.time.Duration;

/**
 * Interface for hook handlers.
 * Implementations execute different types of hooks (command, HTTP, prompt).
 * Extends HookObserver to integrate with the observer + chain pattern.
 */
public interface HookHandler extends HookObserver {

    /**
     * Executes the hook with the given context.
     *
     * @param context the hook context
     * @returns the hook result
     */
    HookResult execute(HookContext context);

    /**
     * Called when a subscribed event is published.
     * Default implementation delegates to execute() for backward compatibility.
     *
     * @param event the event type
     * @param context the event context
     * @param chainContext the chain context for accumulating results
     * @return the hook result
     */
    @Override
    default HookResult onEvent(HookEvent event, HookContext context, ChainContext chainContext) {
        return execute(context);
    }

    /**
     * Gets the type of this hook handler.
     *
     * @return the handler type
     */
    HookHandlerType getType();

    /**
     * Gets the timeout for this handler.
     *
     * @return the timeout duration
     */
    Duration getTimeout();

    /**
     * Gets the command for command hooks.
     * Only valid for COMMAND type handlers.
     *
     * @return the command string
     */
    default String getCommand() {
        return null;
    }

    /**
     * Gets the URL for HTTP hooks.
     * Only valid for HTTP type handlers.
     *
     * @return the URL string
     */
    default String getUrl() {
        return null;
    }

    /**
     * Gets the prompt for prompt hooks.
     * Only valid for PROMPT type handlers.
     *
     * @return the prompt string
     */
    default String getPrompt() {
        return null;
    }

    /**
     * Checks if this handler runs asynchronously.
     *
     * @return true if async
     */
    default boolean isAsync() {
        return false;
    }

    /**
     * Checks if this handler should only run once.
     *
     * @return true if once
     */
    default boolean isOnce() {
        return false;
    }
}
