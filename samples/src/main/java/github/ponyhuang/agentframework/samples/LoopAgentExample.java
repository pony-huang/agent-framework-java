package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.LoopAgent;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.tools.Param;
import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;

import java.util.List;

/**
 * Example showing how to use LoopAgent for multi-turn ReAct execution.
 */
public class LoopAgentExample {

    public static void main(String[] args) {
        // Create tool instance
        CalculatorTool calculator = new CalculatorTool();

        // Create chat client
        ChatClient client = ClientExample.openAIChatClient();

        // Build LoopAgent with custom termination handler using simplified API
        LoopAgent agent = LoopAgent.builder()
                .name("calculator-assistant")
                .instructions("You are a helpful assistant that uses the calculator tool for math operations.")
                .client(client)
                .tool(calculator)
                .maxSteps(5)
                .terminationHandler((fnName, fnArgs, result) -> "task_done".equals(fnName))
                .build();

        // Run the agent
        List<Message> messages = List.of(
                Message.user("Calculate 15 + 27, then multiply the result by 3")
        );

        ChatResponse response = agent.run(messages);

        System.out.println("=== Final Response ===");
        System.out.println(response.getMessage().getText());

        // Also demonstrate using session
        System.out.println("\n=== Using Session ===");
        var session = agent.createSession();
        session.addMessage(Message.user("What is 100 divided by 4?"));
        ChatResponse sessionResponse = session.run(Message.user("What is 100 divided by 4?"));
        System.out.println(sessionResponse.getMessage().getText());
    }

    /**
     * Example tool class with calculator functions.
     */
    public static class CalculatorTool {

        @Tool(name = "calculate", description = "Perform basic arithmetic calculations")
        public String calculate(
                @Param(description = "The operation: add, subtract, multiply, or divide") String operation,
                @Param(description = "First number") double a,
                @Param(description = "Second number") double b
        ) {
            double result;
            switch (operation.toLowerCase()) {
                case "add":
                case "addtion":
                    result = a + b;
                    break;
                case "subtract":
                    result = a - b;
                    break;
                case "multiply":
                    result = a * b;
                    break;
                case "divide":
                    if (b == 0) return "Error: Division by zero";
                    result = a / b;
                    break;
                default:
                    return "Unknown operation: " + operation;
            }
            return String.valueOf(result);
        }

        @Tool(name = "task_done", description = "Mark the task as completed")
        public String taskDone(
                @Param(description = "Whether the task is done", required = false) Boolean done
        ) {
            return "{\"done\": " + (done != null && done) + "}";
        }
    }
}