package github.ponyhuang.agentframework.hooks;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all hook event contexts.
 * Contains common fields shared across all hook events.
 */
public abstract class HookContext {

    protected String sessionId;
    protected String transcriptPath;
    protected String cwd;
    protected String permissionMode;
    protected HookEvent hookEventName;
    protected String agentId;
    protected String agentType;

    protected HookContext(HookEvent hookEventName) {
        this.hookEventName = hookEventName;
    }

    // Common getters and setters

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTranscriptPath() {
        return transcriptPath;
    }

    public void setTranscriptPath(String transcriptPath) {
        this.transcriptPath = transcriptPath;
    }

    public String getCwd() {
        return cwd;
    }

    public void setCwd(String cwd) {
        this.cwd = cwd;
    }

    public String getPermissionMode() {
        return permissionMode;
    }

    public void setPermissionMode(String permissionMode) {
        this.permissionMode = permissionMode;
    }

    public HookEvent getHookEventName() {
        return hookEventName;
    }

    public void setHookEventName(HookEvent hookEventName) {
        this.hookEventName = hookEventName;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    /**
     * Converts this context to a Map for JSON serialization.
     *
     * @return map representation of this context
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("session_id", sessionId);
        map.put("transcript_path", transcriptPath);
        map.put("cwd", cwd);
        map.put("permission_mode", permissionMode);
        map.put("hook_event_name", hookEventName != null ? hookEventName.getEventName() : null);
        if (agentId != null) {
            map.put("agent_id", agentId);
        }
        if (agentType != null) {
            map.put("agent_type", agentType);
        }
        return map;
    }
}
