package github.ponyhuang.agentframework.hooks.event;

/**
 * Event fired when instructions are loaded.
 */
public class InstructionsLoadedEvent extends BaseEvent {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String agentId;
    private String agentType;

    // InstructionsLoaded specific properties
    private String filePath;
    private String memoryType; // User, Project, Local, Managed
    private String loadReason; // session_start, nested_traversal, path_glob_match, include
    private String triggerFilePath;
    private String parentFilePath;

    public InstructionsLoadedEvent() {
        super(HookEventType.INSTRUCTIONS_LOADED);
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
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }

    public String getLoadReason() { return loadReason; }
    public void setLoadReason(String loadReason) { this.loadReason = loadReason; }

    public String getTriggerFilePath() { return triggerFilePath; }
    public void setTriggerFilePath(String triggerFilePath) { this.triggerFilePath = triggerFilePath; }

    public String getParentFilePath() { return parentFilePath; }
    public void setParentFilePath(String parentFilePath) { this.parentFilePath = parentFilePath; }
}
