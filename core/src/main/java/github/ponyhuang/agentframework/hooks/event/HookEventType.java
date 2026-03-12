package github.ponyhuang.agentframework.hooks.event;

/**
 * Enumeration of all hook event types in the agent lifecycle.
 * Matches Claude Code's hook event system.
 */
public enum HookEventType {
    // Session events
    SESSION_START("SessionStart"),
    SESSION_END("SessionEnd"),

    // User interaction
    USER_PROMPT_SUBMIT("UserPromptSubmit"),

    // Tool events
    PRE_TOOL_USE("PreToolUse"),
    POST_TOOL_USE("PostToolUse"),
    POST_TOOL_USE_FAILURE("PostToolUseFailure"),

    // Permission
    PERMISSION_REQUEST("PermissionRequest"),

    // Notifications
    NOTIFICATION("Notification"),

    // Subagent events
    SUBAGENT_START("SubagentStart"),
    SUBAGENT_STOP("SubagentStop"),

    // Agent lifecycle
    STOP("Stop"),
    TEAMMATE_IDLE("TeammateIdle"),
    TASK_COMPLETED("TaskCompleted"),

    // Configuration
    CONFIG_CHANGE("ConfigChange"),
    PRE_COMPACT("PreCompact"),

    // Instructions
    INSTRUCTIONS_LOADED("InstructionsLoaded"),

    // Worktree
    WORKTREE_CREATE("WorktreeCreate"),
    WORKTREE_REMOVE("WorktreeRemove");

    private final String eventName;

    HookEventType(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Gets the event name as used in JSON configuration.
     *
     * @return the event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Finds a HookEventType by its event name.
     *
     * @param eventName the event name
     * @return the matching HookEventType, or null if not found
     */
    public static HookEventType fromEventName(String eventName) {
        for (HookEventType event : values()) {
            if (event.eventName.equals(eventName)) {
                return event;
            }
        }
        return null;
    }
}
