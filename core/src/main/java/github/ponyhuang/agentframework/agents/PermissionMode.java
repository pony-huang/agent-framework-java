package github.ponyhuang.agentframework.agents;

/**
 * Defines tool execution permission levels for the agent.
 * Controls how tools are allowed to execute.
 */
public enum PermissionMode {
    /**
     * Default behavior - hooks can approve or deny tool execution.
     */
    DEFAULT,

    /**
     * Auto-approve file edit operations.
     * Tools that modify files are automatically allowed.
     */
    ACCEPT_EDITS,

    /**
     * Plan only mode - no tool execution allowed.
     * Agent can analyze and plan but cannot execute any tools.
     */
    PLAN,

    /**
     * Bypass all permission checks.
     * All tools are allowed without any permission verification.
     */
    BYPASS
}
