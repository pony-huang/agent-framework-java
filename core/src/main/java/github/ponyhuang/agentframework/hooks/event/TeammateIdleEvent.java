package github.ponyhuang.agentframework.hooks.event;

/**
 * Event fired when a teammate becomes idle.
 */
public class TeammateIdleEvent extends BaseEvent {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String agentId;
    private String agentType;

    // TeammateIdle specific properties
    private String teammateName;
    private String teamName;

    public TeammateIdleEvent() {
        super(HookEventType.TEAMMATE_IDLE);
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
    public String getTeammateName() { return teammateName; }
    public void setTeammateName(String teammateName) { this.teammateName = teammateName; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
}
