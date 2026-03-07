package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for PostToolUse hook event.
 * Fired after a tool executes successfully.
 */
public class PostToolUseContext extends HookContext {

    private String toolName;
    private Map<String, Object> toolInput;
    private Map<String, Object> toolResponse;
    private String toolUseId;

    public PostToolUseContext() {
        super(HookEvent.POST_TOOL_USE);
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

    public Map<String, Object> getToolResponse() {
        return toolResponse;
    }

    public void setToolResponse(Map<String, Object> toolResponse) {
        this.toolResponse = toolResponse;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("tool_name", toolName);
        map.put("tool_input", toolInput);
        map.put("tool_response", toolResponse);
        map.put("tool_use_id", toolUseId);
        return map;
    }
}
