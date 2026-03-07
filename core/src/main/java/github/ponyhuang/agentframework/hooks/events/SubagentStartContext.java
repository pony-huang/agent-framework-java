package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for SubagentStart hook event.
 * Fired when a subagent is spawned.
 */
public class SubagentStartContext extends HookContext {

    private String agentId;
    private String agentType;

    public SubagentStartContext() {
        super(HookEvent.SUBAGENT_START);
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    @Override
    public String getAgentType() {
        return agentType;
    }

    @Override
    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("agent_id", agentId);
        map.put("agent_type", agentType);
        return map;
    }
}
