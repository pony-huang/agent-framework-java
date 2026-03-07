package github.ponyhuang.agentframework.hooks;

import github.ponyhuang.agentframework.hooks.events.PreToolUseContext;

import java.time.Duration;

/**
 * Interface for hook handlers.
 * Implementations execute different types of hooks (command, HTTP, prompt).
 */
public interface HookHandler {

    /**
     * Executes the hook with the given context.
     *
     * @param context the hook context
     * @return the hook result
     */
    HookResult execute(HookContext context);

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
