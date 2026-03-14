package github.ponyhuang.agentframework.hooks.event;

import github.ponyhuang.agentframework.sessions.Session;

import java.util.Map;

/**
 * Event fired when a session begins or resumes.
 */
public class SessionStartEvent extends BaseEvent implements HasMatcherValue {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String agentId;
    private String agentType;

    // SessionStart specific properties
    private String source; // startup, resume, clear, compact
    private String model;
    private Session session; // Full session object for session-based runs

    public SessionStartEvent() {
        super(HookEventType.SESSION_START);
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

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }

    @Override
    public String getMatcherValue() {
        return source;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("session_id", sessionId);
        map.put("transcript_path", transcriptPath);
        map.put("cwd", cwd);
        map.put("permission_mode", permissionMode);
        map.put("agent_id", agentId);
        map.put("agent_type", agentType);
        map.put("source", source);
        map.put("model", model);
        return map;
    }
}
