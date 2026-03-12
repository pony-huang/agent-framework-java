package github.ponyhuang.agentframework.hooks.event;

/**
 * Event fired when a subagent starts.
 */
public class SubagentStartEvent extends BaseEvent implements HasMatcherValue {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String agentId;
    private String agentType;

    // SubagentStart specific properties
    private String agentIdValue;
    private String agentTypeValue;

    public SubagentStartEvent() {
        super(HookEventType.SUBAGENT_START);
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
    public String getAgentId() { return agentIdValue; }
    public void setAgentId(String agentId) { this.agentIdValue = agentId; }

    public String getAgentType() { return agentTypeValue; }
    public void setAgentType(String agentType) { this.agentTypeValue = agentType; }

    @Override
    public String getMatcherValue() {
        return agentTypeValue;
    }
}
