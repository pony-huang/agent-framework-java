package github.ponyhuang.agentframework.tools;

/**
 * Handler for tool approval requests.
 * When a tool requires approval before execution, this handler is called
 * to determine whether the tool should be executed.
 */
@FunctionalInterface
public interface ToolApprovalHandler {

    /**
     * Called when a tool requires approval before execution.
     *
     * @param toolName the name of the tool to approve
     * @param arguments the arguments that will be passed to the tool
     * @return true if the tool execution is approved, false to reject
     */
    boolean approve(String toolName, java.util.Map<String, Object> arguments);
}
