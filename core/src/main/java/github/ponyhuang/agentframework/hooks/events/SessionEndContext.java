package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for SessionEnd hook event.
 * Fired when a session terminates.
 */
public class SessionEndContext extends HookContext {

    private String reason; // clear, logout, prompt_input_exit, bypass_permissions_disabled, other

    public SessionEndContext() {
        super(HookEvent.SESSION_END);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("reason", reason);
        return map;
    }
}
