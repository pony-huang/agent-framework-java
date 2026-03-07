package github.ponyhuang.agentframework.hooks;

import java.util.Map;

/**
 * Result returned from hook execution.
 * Contains decision, output, and control fields.
 */
public class HookResult {

    private boolean allow = true;
    private String reason;
    private boolean shouldContinue = true;
    private String stopReason;
    private boolean suppressOutput = false;
    private String systemMessage;
    private Map<String, Object> hookSpecificOutput;
    private String additionalContext;

    public HookResult() {
    }

    public static HookResult allow() {
        HookResult result = new HookResult();
        result.setAllow(true);
        return result;
    }

    public static HookResult deny(String reason) {
        HookResult result = new HookResult();
        result.setAllow(false);
        result.setReason(reason);
        return result;
    }

    public static HookResult block(String reason) {
        return deny(reason);
    }

    // Getters and setters

    public boolean isAllow() {
        return allow;
    }

    public void setAllow(boolean allow) {
        this.allow = allow;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isShouldContinue() {
        return shouldContinue;
    }

    public void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public boolean isSuppressOutput() {
        return suppressOutput;
    }

    public void setSuppressOutput(boolean suppressOutput) {
        this.suppressOutput = suppressOutput;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }

    public Map<String, Object> getHookSpecificOutput() {
        return hookSpecificOutput;
    }

    public void setHookSpecificOutput(Map<String, Object> hookSpecificOutput) {
        this.hookSpecificOutput = hookSpecificOutput;
    }

    public String getAdditionalContext() {
        return additionalContext;
    }

    public void setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
    }

    /**
     * Checks if this result indicates a blocking error (exit code 2).
     */
    public boolean isBlockingError() {
        return !allow && reason != null;
    }
}
