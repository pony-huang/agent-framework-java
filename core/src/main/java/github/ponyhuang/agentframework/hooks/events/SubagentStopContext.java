package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for SubagentStop hook event.
 * Fired when a subagent finishes responding.
 */
public class SubagentStopContext extends HookContext {

    private boolean stopHookActive;
    private String agentId;
    private String agentType;
    private String agentTranscriptPath;
    private String lastAssistantMessage;

    public SubagentStopContext() {
        super(HookEvent.SUBAGENT_STOP);
    }

    public boolean isStopHookActive() {
        return stopHookActive;
    }

    public void setStopHookActive(boolean stopHookActive) {
        this.stopHookActive = stopHookActive;
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

    public String getAgentTranscriptPath() {
        return agentTranscriptPath;
    }

    public void setAgentTranscriptPath(String agentTranscriptPath) {
        this.agentTranscriptPath = agentTranscriptPath;
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
        map.put("agent_id", agentId);
        map.put("agent_type", agentType);
        map.put("agent_transcript_path", agentTranscriptPath);
        map.put("last_assistant_message", lastAssistantMessage);
        return map;
    }
}
