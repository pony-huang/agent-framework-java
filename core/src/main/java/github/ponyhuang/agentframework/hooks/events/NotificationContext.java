package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for Notification hook event.
 * Fired when a notification is sent.
 */
public class NotificationContext extends HookContext {

    private String message;
    private String title;
    private String notificationType; // permission_prompt, idle_prompt, auth_success, elicitation_dialog

    public NotificationContext() {
        super(HookEvent.NOTIFICATION);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("message", message);
        map.put("title", title);
        map.put("notification_type", notificationType);
        return map;
    }
}
