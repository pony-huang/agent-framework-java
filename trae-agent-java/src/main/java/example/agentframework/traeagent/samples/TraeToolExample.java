package example.agentframework.traeagent.samples;

import example.agentframework.traeagent.tools.BashTool;
import example.agentframework.traeagent.tools.EditTool;
import example.agentframework.traeagent.tools.JSONEditTool;
import example.agentframework.traeagent.tools.SequentialThinkingTool;
import example.agentframework.traeagent.tools.TaskDoneTool;

/**
 * Example showing how to use individual tools directly.
 *
 * This example demonstrates using tools without the full agent:
 * - BashTool for shell commands
 * - EditTool for file operations
 * - JSONEditTool for JSON manipulation
 * - SequentialThinkingTool for reasoning
 * - TaskDoneTool for task completion
 */
public class TraeToolExample {

    public static void main(String[] args) {
        String workingDir = System.getProperty("user.dir");

        // Create tool instances
        BashTool bashTool = new BashTool(workingDir);
        EditTool editTool = new EditTool(workingDir);
        JSONEditTool jsonEditTool = new JSONEditTool(workingDir);
        SequentialThinkingTool thinkingTool = new SequentialThinkingTool();
        TaskDoneTool taskDoneTool = new TaskDoneTool();

        System.out.println("=== Trae Tools Example ===\n");

        // 1. Use BashTool
        System.out.println("1. Bash Tool - List files:");
        String listResult = bashTool.bash("ls -la");
        System.out.println(listResult);

        // 2. Use EditTool - create a file
        System.out.println("\n2. Edit Tool - Create a file:");
        String createResult = editTool.create("test.txt", "Hello from TraeAgent!");
        System.out.println(createResult);

        // 3. Use EditTool - view a file
        System.out.println("\n3. Edit Tool - View the file:");
        String viewResult = editTool.view("test.txt");
        System.out.println(viewResult);

        // 4. Use EditTool - edit file content
        System.out.println("\n4. Edit Tool - Edit file:");
        String editResult = editTool.strReplace("test.txt", "Hello", "Greetings");
        System.out.println(editResult);

        // 5. Use JSONEditTool - create JSON via bash first
        System.out.println("\n5. JSON Edit Tool - Create JSON via bash:");
        bashTool.bash("echo '{\"name\": \"test\", \"version\": \"1.0\"}' > config.json");
        String jsonViewResult = jsonEditTool.jsonView("config.json");
        System.out.println(jsonViewResult);

        System.out.println("\n6. JSON Edit Tool - Update JSON:");
        String jsonUpdateResult = jsonEditTool.jsonSet("config.json", "version", "\"2.0\"");
        System.out.println(jsonUpdateResult);

        System.out.println("\n7. JSON Edit Tool - View JSON:");
        jsonViewResult = jsonEditTool.jsonView("config.json");
        System.out.println(jsonViewResult);

        // 8. Use SequentialThinkingTool
        System.out.println("\n8. Sequential Thinking Tool:");
        String thinkResult = thinkingTool.think(
                "I need to solve this problem step by step",
                "think",
                "Find the solution"
        );
        System.out.println(thinkResult);

        // 9. Use TaskDoneTool
        System.out.println("\n9. Task Done Tool:");
        java.util.Map<String, Object> doneResult = taskDoneTool.taskDone(
                true, // done
                "Successfully completed all examples", // summary
                false, // mustPatch
                null // remaining
        );
        System.out.println(doneResult);

        // Cleanup
        System.out.println("\n10. Cleanup - Delete test files:");
        bashTool.bash("rm -f test.txt config.json");

        System.out.println("\n=== Example Complete ===");
    }
}