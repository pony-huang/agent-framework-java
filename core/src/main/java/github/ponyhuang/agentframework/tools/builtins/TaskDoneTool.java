package github.ponyhuang.agentframework.tools.builtins;

import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Built-in tool for signaling task completion.
 * When called, it signals to the LoopAgent that the task is complete.
 */
public class TaskDoneTool {

    private static final Logger LOG = LoggerFactory.getLogger(TaskDoneTool.class);

    /**
     * Marker to indicate the task is complete.
     * The LoopAgent checks for this specific result to terminate the loop.
     */
    public static final String TASK_COMPLETE_MARKER = "__TASK_DONE__";

    /**
     * Signal that the task is complete.
     * Call this tool when the agent has finished its work.
     */
    @Tool(description = "Signal that the task is complete. " +
            "Call this tool when you have finished the user's request. " +
            "The result will be returned to the user.")
    public String taskDone(
            @ToolParam(description = "The result or summary of what was accomplished", required = false) String result) {

        String message = result != null && !result.isEmpty()
                ? result
                : "Task completed successfully";

        LOG.info("Task completed with result: {}", message);

        // Return a special marker that LoopAgent uses to detect completion
        return TASK_COMPLETE_MARKER + "|" + message;
    }

    /**
     * Check if the result indicates task completion.
     *
     * @param result the tool result
     * @return true if the result indicates task completion
     */
    public static boolean isTaskComplete(String result) {
        return result != null && result.startsWith(TASK_COMPLETE_MARKER);
    }

    /**
     * Extract the message from a task done result.
     *
     * @param result the tool result
     * @return the message, or null if not a task done result
     */
    public static String extractMessage(String result) {
        if (!isTaskComplete(result)) {
            return null;
        }
        String message = result.substring(TASK_COMPLETE_MARKER.length() + 1);
        return message.isEmpty() ? "Task completed successfully" : message;
    }
}
