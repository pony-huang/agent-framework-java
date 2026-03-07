package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for PreCompact hook event.
 * Fired before context compaction.
 */
public class PreCompactContext extends HookContext {

    private String trigger; // manual, auto
    private String customInstructions;

    public PreCompactContext() {
        super(HookEvent.PRE_COMPACT);
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getCustomInstructions() {
        return customInstructions;
    }

    public void setCustomInstructions(String customInstructions) {
        this.customInstructions = customInstructions;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("trigger", trigger);
        map.put("custom_instructions", customInstructions);
        return map;
    }
}
