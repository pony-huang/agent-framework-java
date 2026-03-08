package example.agentframework.codeagent.tools;

import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolParam;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for signaling task completion.
 * The agent calls this when it believes the task is done.
 */
public class TaskDoneTool {

    /**
     * Signal that the task is complete.
     *
     * @param done Whether the task is complete
     * @param summary Summary of what was accomplished
     * @param mustPatch Whether a patch is required (for code changes)
     * @param remaining Remaining issues or tasks (if not done)
     * @return Result message
     */
    @Tool(description = "Signal that the task is complete or not. " +
            "Call this when you believe the work is finished. " +
            "Set done=true when the task is complete, false if there are remaining issues. " +
            "Use must_patch=true if the task requires code changes.")
    public Map<String, Object> taskDone(
            @ToolParam(description = "Whether the task is complete", required = false) Boolean done,
            @ToolParam(description = "Summary of what was accomplished", required = false) String summary,
            @ToolParam(description = "Whether a patch is required for this task", required = false) Boolean mustPatch,
            @ToolParam(description = "Remaining issues if not done", required = false) String remaining) {

        Map<String, Object> result = new HashMap<>();

        boolean isDone = done != null && done;
        result.put("done", isDone);

        if (summary != null && !summary.isEmpty()) {
            result.put("summary", summary);
        }

        if (mustPatch != null) {
            result.put("must_patch", mustPatch);
        }

        if (remaining != null && !remaining.isEmpty()) {
            result.put("remaining", remaining);
        }

        if (isDone) {
            result.put("message", "Task marked as complete: " + (summary != null ? summary : ""));
        } else {
            result.put("message", "Task not complete. Remaining: " + (remaining != null ? remaining : "unknown"));
        }

        return result;
    }

    /**
     * Quick task done with just completion status.
     */
    @Tool(description = "Quick way to mark task as done or not done.")
    public Map<String, Object> done(
            @ToolParam(description = "Whether the task is complete") boolean isDone) {

        Map<String, Object> result = new HashMap<>();
        result.put("done", isDone);
        result.put("message", isDone ? "Task completed." : "Task not completed.");
        return result;
    }

    /**
     * Request confirmation from user before marking complete.
     */
    @Tool(description = "Request confirmation from the user before finalizing task completion.")
    public Map<String, Object> requestConfirmation(
            @ToolParam(description = "Question or item to confirm") String question) {

        Map<String, Object> result = new HashMap<>();
        result.put("type", "confirmation_requested");
        result.put("question", question);
        result.put("done", false);
        result.put("message", "Confirmation needed: " + question);
        return result;
    }

    /**
     * Report progress on a multi-step task.
     */
    @Tool(description = "Report progress on the current task. Use for multi-step tasks.")
    public Map<String, Object> progress(
            @ToolParam(description = "Current progress description") String progress,
            @ToolParam(description = "Total steps or items", required = false) Integer total,
            @ToolParam(description = "Current step or item", required = false) Integer current) {

        Map<String, Object> result = new HashMap<>();
        result.put("type", "progress");
        result.put("progress", progress);

        if (total != null && current != null) {
            result.put("total", total);
            result.put("current", current);
            result.put("percentage", (current * 100) / total);
        }

        result.put("done", false);
        result.put("message", "Progress: " + progress);
        return result;
    }
}