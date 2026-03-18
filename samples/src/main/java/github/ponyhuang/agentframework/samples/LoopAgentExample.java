package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.LoopAgent;
import github.ponyhuang.agentframework.agents.PermissionMode;
import github.ponyhuang.agentframework.providers.ChatClient;
import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.TextBlock;
import github.ponyhuang.agentframework.types.block.ThinkingBlock;
import github.ponyhuang.agentframework.types.block.ToolResultBlock;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;

import java.util.List;
import java.util.Set;

/**
 * Example showing how to use LoopAgent for multi-turn ReAct execution.
 */
public class LoopAgentExample {

    public static void main(String[] args) {

        // Run the agent with comprehensive tool verification request
        String userMessage = """
                Please help me verify all available tools are working correctly. Execute the following tasks in order:
                
                ## 1. File Operations Test
                - Use `glob` to find all Java files in the current directory matching "*.java"
                - Use `write` to create a test file named "test_output.txt" with content "Hello, Tool Testing!"
                - Use `read` to read the contents of "test_output.txt"
                - Use `readRange` to read lines 1-2 from "test_output.txt"
                - Use `edit` to replace "Hello" with "Hi" in "test_output.txt"
                - Use `read` again to verify the edit was successful
                
                ## 2. Shell Commands Test
                - Use `bash` to execute: "echo 'Shell command works!'"
                - Use `bashDetailed` to execute: "pwd" and show me the detailed result with exit code
                - Use `bashWithTimeout` to execute: "sleep 2 && echo 'Timeout command works'" with timeout=5
                
                ## 3. Task Management Test
                - Use `create` to create a task with subject "Test Task 1" and description "Testing task creation"
                - Use `createWithOptions` to create another task with subject "Test Task 2", description "Advanced task", and activeForm "Creating advanced task"
                - Use `list` to show all tasks
                - Use `get` to retrieve task ID "1"
                - Use `update` to update task ID "1" status to "in_progress"
                - Use `delete` to delete task ID "2"
                - Use `list` again to verify the deletion
                
                ## 4. System Tools Test
                - Use `askUser` to ask: "Which tool category do you find most useful?" with options: "File Operations,Shell Commands,Task Management"
                - Use `planMode` with reason: "Planning the implementation approach for the remaining tasks"
                
                ## 5. Completion
                - After completing all the above tests, use `task_done` with a summary report of which tools executed successfully and any errors encountered
                
                Please execute each step sequentially and show me the results. If any tool fails, note the error and continue with the next tool.
                """;
        ChatClient client = ClientExample.openAIChatClient();

        LoopAgent agent = LoopAgent.builder()
                .name("assistant")
                .instructions("You are a helpful assistant.")
                .client(client)
                .maxSteps(30)
                // New alignment features:
                .maxBudgetUsd(1.00)  // Limit costs to $1.00
                .fallbackModel("claude-haiku-3-5")  // Fallback if primary fails
                .permissionMode(PermissionMode.DEFAULT)  // Allow tool execution
                //.disallowedTools(Set.of("Bash"))  // Uncomment to block specific tools
                .build();

        // Run the agent
        List<Message> messages = List.of(
                UserMessage.create(userMessage)
        );

        agent.runStream(messages).subscribe(
                response -> {
                    // Print all blocks in the message
                    List<Block> blocks = response.getBlocks();
                    if (blocks != null) {
                        for (Block block : blocks) {
                            if (block instanceof TextBlock) {
                                System.out.println("=== Text ===");
                                System.out.println(((TextBlock) block).getText());
                            } else if (block instanceof ThinkingBlock) {
                                System.out.println("=== Thinking ===");
                                System.out.println(((ThinkingBlock) block).getThinking());
                            } else if (block instanceof ToolUseBlock toolUse) {
                                System.out.println("=== Tool Call ===");
                                System.out.println("Tool: " + toolUse.getName());
                                System.out.println("Input: " + toolUse.getInput());
                            } else if (block instanceof ToolResultBlock toolResult) {
                                System.out.println("=== Tool Result ===");
                                System.out.println("Tool ID: " + toolResult.getToolUseId());
                                System.out.println("Result: " + toolResult.getContent());
                            }
                        }
                    }
                },
                error -> System.err.println("Error: " + error.getMessage()),
                () -> System.out.println("=== Stream Completed ===")
        );


    }
}