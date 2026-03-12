package github.ponyhuang.agentframework.hooks.event;

/**
 * Event fired when a subagent stops.
 */
public class SubagentStopEvent extends BaseEvent implements HasMatcherValue {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;

    // SubagentStop specific properties
    private boolean stopHookActive;
    private String agentId;
    private String agentType;
    private String agentTranscriptPath;
    private String lastAssistantMessage;

    public SubagentStopEvent() {
        super(HookEventType.SUBAGENT_STOP);
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

    // Specific getters and setters
    public boolean isStopHookActive() { return stopHookActive; }
    public void setStopHookActive(boolean stopHookActive) { this.stopHookActive = stopHookActive; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getAgentTranscriptPath() { return agentTranscriptPath; }
    public void setAgentTranscriptPath(String agentTranscriptPath) { this.agentTranscriptPath = agentTranscriptPath; }

    public String getLastAssistantMessage() { return lastAssistantMessage; }
    public void setLastAssistantMessage(String lastAssistantMessage) { this.lastAssistantMessage = lastAssistantMessage; }

    @Override
    public String getMatcherValue() {
        return agentType;
    }
}
