package github.ponyhuang.agentframework.hooks.event;

/**
 * Event fired when configuration changes.
 */
public class ConfigChangeEvent extends BaseEvent implements HasMatcherValue {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String agentId;
    private String agentType;

    // ConfigChange specific properties
    private String source; // user_settings, project_settings, local_settings, policy_settings, skills
    private String filePath;

    public ConfigChangeEvent() {
        super(HookEventType.CONFIG_CHANGE);
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
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public String getMatcherValue() {
        return source;
    }
}
