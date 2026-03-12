package github.ponyhuang.agentframework.hooks.event;

import java.util.Map;

/**
 * Event fired before a tool is executed.
 */
public class PreToolUseEvent extends BaseEvent implements HasMatcherValue {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String agentId;
    private String agentType;

    // PreToolUse specific properties
    private String toolName;
    private Map<String, Object> toolInput;
    private String toolUseId;

    public PreToolUseEvent() {
        super(HookEventType.PRE_TOOL_USE);
    }

    // Common getters and setters
    public String getSessionId() { return sessionId; }
    public PreToolUseEvent setSessionId(String sessionId) { this.sessionId = sessionId; return this; }

    public String getTranscriptPath() { return transcriptPath; }
    public PreToolUseEvent setTranscriptPath(String transcriptPath) { this.transcriptPath = transcriptPath; return this; }

    public String getCwd() { return cwd; }
    public PreToolUseEvent setCwd(String cwd) { this.cwd = cwd; return this; }

    public String getPermissionMode() { return permissionMode; }
    public PreToolUseEvent setPermissionMode(String permissionMode) { this.permissionMode = permissionMode; return this; }

    public String getAgentId() { return agentId; }
    public PreToolUseEvent setAgentId(String agentId) { this.agentId = agentId; return this; }

    public String getAgentType() { return agentType; }
    public PreToolUseEvent setAgentType(String agentType) { this.agentType = agentType; return this; }

    // Specific getters and setters
    public String getToolName() { return toolName; }
    public PreToolUseEvent setToolName(String toolName) { this.toolName = toolName; return this; }

    public Map<String, Object> getToolInput() { return toolInput; }
    public PreToolUseEvent setToolInput(Map<String, Object> toolInput) { this.toolInput = toolInput; return this; }

    public String getToolUseId() { return toolUseId; }
    public PreToolUseEvent setToolUseId(String toolUseId) { this.toolUseId = toolUseId; return this; }

    @Override
    public String getMatcherValue() {
        return toolName;
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
        map.put("tool_name", toolName);
        map.put("tool_input", toolInput);
        map.put("tool_use_id", toolUseId);
        return map;
    }
}
