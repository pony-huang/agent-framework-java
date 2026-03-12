package github.ponyhuang.agentframework.hooks.event;

import java.util.Map;

/**
 * Event fired when the agent finishes responding.
 */
public class StopEvent extends BaseEvent {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String agentId;
    private String agentType;

    // Stop specific properties
    private boolean stopHookActive;
    private String lastAssistantMessage;

    public StopEvent() {
        super(HookEventType.STOP);
    }

    // Common getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTranscriptPath() { return transcriptPath; }
    public void setTranscriptPath(String transcriptPath) { this.transcriptPath = transcriptPath; }

    public String getCwd() { return cwd; }
    public void setCwd(String cwd) { this.cwd = cwd; }

    public String getPermissionMode() { return permissionMode; }
    public void setPermissionMode(String permissionMode) { this.permissionMode = permissionMode; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    // Specific getters and setters
    public boolean isStopHookActive() { return stopHookActive; }
    public void setStopHookActive(boolean stopHookActive) { this.stopHookActive = stopHookActive; }

    public String getLastAssistantMessage() { return lastAssistantMessage; }
    public void setLastAssistantMessage(String lastAssistantMessage) { this.lastAssistantMessage = lastAssistantMessage; }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("session_id", sessionId);
        map.put("transcript_path", transcriptPath);
        map.put("cwd", cwd);
        map.put("permission_mode", permissionMode);
        map.put("agent_id", agentId);
        map.put("agent_type", agentType);
        map.put("stop_hook_active", stopHookActive);
        map.put("last_assistant_message", lastAssistantMessage);
        return map;
    }
}
