package github.ponyhuang.agentframework.hooks.event;

import java.util.List;
import java.util.Map;

/**
 * Event fired when a permission dialog is about to be shown.
 */
public class PermissionRequestEvent extends BaseEvent implements HasMatcherValue {

    // Common properties from HookContext (下沉)
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String agentId;
    private String agentType;

    // PermissionRequest specific properties
    private String toolName;
    private Map<String, Object> toolInput;
    private List<Map<String, Object>> permissionSuggestions;

    public PermissionRequestEvent() {
        super(HookEventType.PERMISSION_REQUEST);
    }

    // Common getters and setters
    public String getSessionId() { return sessionId; }
    public PermissionRequestEvent setSessionId(String sessionId) { this.sessionId = sessionId; return this; }

    public String getTranscriptPath() { return transcriptPath; }
    public PermissionRequestEvent setTranscriptPath(String transcriptPath) { this.transcriptPath = transcriptPath; return this; }

    public String getCwd() { return cwd; }
    public PermissionRequestEvent setCwd(String cwd) { this.cwd = cwd; return this; }

    public String getPermissionMode() { return permissionMode; }
    public PermissionRequestEvent setPermissionMode(String permissionMode) { this.permissionMode = permissionMode; return this; }

    public String getAgentId() { return agentId; }
    public PermissionRequestEvent setAgentId(String agentId) { this.agentId = agentId; return this; }

    public String getAgentType() { return agentType; }
    public PermissionRequestEvent setAgentType(String agentType) { this.agentType = agentType; return this; }

    // Specific getters and setters
    public String getToolName() { return toolName; }
    public PermissionRequestEvent setToolName(String toolName) { this.toolName = toolName; return this; }

    public Map<String, Object> getToolInput() { return toolInput; }
    public PermissionRequestEvent setToolInput(Map<String, Object> toolInput) { this.toolInput = toolInput; return this; }

    public List<Map<String, Object>> getPermissionSuggestions() { return permissionSuggestions; }
    public PermissionRequestEvent setPermissionSuggestions(List<Map<String, Object>> permissionSuggestions) { this.permissionSuggestions = permissionSuggestions; return this; }

    @Override
    public String getMatcherValue() {
        return toolName;
    }
}
