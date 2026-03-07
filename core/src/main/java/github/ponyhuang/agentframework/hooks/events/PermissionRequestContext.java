package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.List;
import java.util.Map;

/**
 * Context for PermissionRequest hook event.
 * Fired when a permission dialog is about to be shown.
 */
public class PermissionRequestContext extends HookContext {

    private String toolName;
    private Map<String, Object> toolInput;
    private List<Map<String, Object>> permissionSuggestions;

    public PermissionRequestContext() {
        super(HookEvent.PERMISSION_REQUEST);
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getToolInput() {
        return toolInput;
    }

    public void setToolInput(Map<String, Object> toolInput) {
        this.toolInput = toolInput;
    }

    public List<Map<String, Object>> getPermissionSuggestions() {
        return permissionSuggestions;
    }

    public void setPermissionSuggestions(List<Map<String, Object>> permissionSuggestions) {
        this.permissionSuggestions = permissionSuggestions;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("tool_name", toolName);
        map.put("tool_input", toolInput);
        map.put("permission_suggestions", permissionSuggestions);
        return map;
    }
}
