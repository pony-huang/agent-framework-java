package github.ponyhuang.agentframework.hooks;

import github.ponyhuang.agentframework.hooks.event.*;

/**
 * Utility class for creating events with a fluent API.
 */
public class EventBuilder {

    private EventBuilder() {
        // Utility class
    }

    /**
     * Creates a new event builder for the specified event type.
     *
     * @param eventClass the event class to create
     * @param <T> the event type
     * @return the event builder
     */
    public static <T extends BaseEvent> T create(Class<T> eventClass) {
        try {
            return eventClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create event: " + eventClass.getName(), e);
        }
    }

    // Convenience methods for common events

    public static PreToolUseEvent preToolUse() {
        return new PreToolUseEvent();
    }

    public static PostToolUseEvent postToolUse() {
        return new PostToolUseEvent();
    }

    public static PostToolUseFailureEvent postToolUseFailure() {
        return new PostToolUseFailureEvent();
    }

    public static PermissionRequestEvent permissionRequest() {
        return new PermissionRequestEvent();
    }

    public static SessionStartEvent sessionStart() {
        return new SessionStartEvent();
    }

    public static SessionEndEvent sessionEnd() {
        return new SessionEndEvent();
    }

    public static StopEvent stop() {
        return new StopEvent();
    }

    public static UserPromptSubmitEvent userPromptSubmit() {
        return new UserPromptSubmitEvent();
    }

    public static NotificationEvent notification() {
        return new NotificationEvent();
    }

    public static SubagentStartEvent subagentStart() {
        return new SubagentStartEvent();
    }

    public static SubagentStopEvent subagentStop() {
        return new SubagentStopEvent();
    }

    public static TaskCompletedEvent taskCompleted() {
        return new TaskCompletedEvent();
    }

    public static ConfigChangeEvent configChange() {
        return new ConfigChangeEvent();
    }

    public static PreCompactEvent preCompact() {
        return new PreCompactEvent();
    }

    public static InstructionsLoadedEvent instructionsLoaded() {
        return new InstructionsLoadedEvent();
    }

    public static WorktreeCreateEvent worktreeCreate() {
        return new WorktreeCreateEvent();
    }

    public static WorktreeRemoveEvent worktreeRemove() {
        return new WorktreeRemoveEvent();
    }

    public static TeammateIdleEvent teammateIdle() {
        return new TeammateIdleEvent();
    }
}
