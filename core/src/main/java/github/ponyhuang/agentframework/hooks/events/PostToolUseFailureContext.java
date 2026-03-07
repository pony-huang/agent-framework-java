package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for PostToolUseFailure hook event.
 * Fired when a tool execution fails.
 */
public class PostToolUseFailureContext extends HookContext {

    private String toolName;
    private Map<String, Object> toolInput;
    private String toolUseId;
    private String error;
    private boolean isInterrupt;

    public PostToolUseFailureContext() {
        super(HookEvent.POST_TOOL_USE_FAILURE);
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

    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isInterrupt() {
        return isInterrupt;
    }

    public void setInterrupt(boolean interrupt) {
        isInterrupt = interrupt;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("tool_name", toolName);
        map.put("tool_input", toolInput);
        map.put("tool_use_id", toolUseId);
        map.put("error", error);
        map.put("is_interrupt", isInterrupt);
        return map;
    }
}
