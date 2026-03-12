package github.ponyhuang.agentframework.hooks.event;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all hook events.
 * <p>
 * Provides common functionality including type identification and timestamp tracking.
 * All concrete event implementations should extend this base class.
 * </p>
 */
public abstract class BaseEvent {

    private final HookEventType type;
    private final long timestamp;
    private Object rawEvent;

    /**
     * Creates a new BaseEvent with the specified type.
     * <p>
     * The timestamp is automatically set to the current time in milliseconds
     * since epoch.
     * </p>
     *
     * @param type the type of this event
     */
    public BaseEvent(HookEventType type) {
        this.type = type;
        this.timestamp = Instant.now().toEpochMilli();
    }

    /**
     * Returns the type of this event.
     *
     * @return the event type
     */
    public HookEventType getType() {
        return type;
    }

    /**
     * Returns the timestamp when this event occurred.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the raw event object containing the original event data.
     *
     * @param rawEvent the raw event data
     */
    public void setRawEvent(Object rawEvent) {
        this.rawEvent = rawEvent;
    }

    /**
     * Returns the raw event object containing the original event data.
     *
     * @return the raw event data
     */
    public Object getRawEvent() {
        return rawEvent;
    }

    /**
     * Converts this event to a Map for JSON serialization.
     * Subclasses should override this to include their specific properties.
     *
     * @return map representation of this event
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("hook_event_name", type != null ? type.getEventName() : null);
        map.put("timestamp", timestamp);
        return map;
    }
}
