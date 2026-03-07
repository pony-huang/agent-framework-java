package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for Stop hook event.
 * Fired when the agent finishes responding.
 */
public class StopContext extends HookContext {

    private boolean stopHookActive;
    private String lastAssistantMessage;

    public StopContext() {
        super(HookEvent.STOP);
    }

    public boolean isStopHookActive() {
        return stopHookActive;
    }

    public void setStopHookActive(boolean stopHookActive) {
        this.stopHookActive = stopHookActive;
    }

    public String getLastAssistantMessage() {
        return lastAssistantMessage;
    }

    public void setLastAssistantMessage(String lastAssistantMessage) {
        this.lastAssistantMessage = lastAssistantMessage;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("stop_hook_active", stopHookActive);
        map.put("last_assistant_message", lastAssistantMessage);
        return map;
    }
}
