//package github.ponyhuang.agentframework.samples;
//
//import github.ponyhuang.agentframework.agents.LoopAgent;
//import github.ponyhuang.agentframework.clients.ChatClient;
//import github.ponyhuang.agentframework.tools.Tool;
//import github.ponyhuang.agentframework.tools.ToolParam;
//import github.ponyhuang.agentframework.types.ChatResponse;
//import github.ponyhuang.agentframework.types.message.Message;
//import github.ponyhuang.agentframework.types.message.UserMessage;
//
//import java.util.List;
//
///**
// * Example showing how to use LoopAgent for multi-turn ReAct execution.
// */
//public class LoopAgentExample {
//
//    public static void main(String[] args) {
//        CalculatorTool calculator = new CalculatorTool();
//
//        ChatClient client = ClientExample.openAIChatClient();
//
//        LoopAgent agent = LoopAgent.builder()
//                .name("calculator-assistant")
//                .instructions("You are a helpful assistant that uses the calculator tool for math operations.")
//                .client(client)
//                .tool(calculator)
//                .maxSteps(5)
//                .build();
//
//        // Run the agent
//        List<Message> messages = List.of(
//                UserMessage.create("Calculate 15 + 27, then multiply the result by 3")
//        );
//
//        ChatResponse response = agent.run(messages, new java.util.HashMap<>());
//
//        System.out.println("=== Final Response ===");
//        System.out.println(response.getLastMessage().getTextContent());
//
//        // Also demonstrate using session
//        System.out.println("\n=== Using Session ===");
//        var session = agent.createSession();
//        session.addMessage(UserMessage.create("What is 100 divided by 4?"));
//        List<Message> sessionMessages = session.runStream(session, UserMessage.create("What is 100 divided by 4?")).collectList().block();
//        ChatResponse sessionResponse = ChatResponse.builder().messages(sessionMessages).build();
//        System.out.println(sessionResponse.getLastMessage().getTextContent());
//    }
//
//    /**
//     * Example tool class with calculator functions.
//     */
//    public static class CalculatorTool {
//
//        @Tool(name = "calculate", description = "Perform basic arithmetic calculations")
//        public String calculate(
//                @ToolParam(description = "The operation: add, subtract, multiply, or divide") String operation,
//                @ToolParam(description = "First number") double a,
//                @ToolParam(description = "Second number") double b
//        ) {
//            double result;
//            switch (operation.toLowerCase()) {
//                case "add":
//                case "addtion":
//                    result = a + b;
//                    break;
//                case "subtract":
//                    result = a - b;
//                    break;
//                case "multiply":
//                    result = a * b;
//                    break;
//                case "divide":
//                    if (b == 0) return "Error: Division by zero";
//                    result = a / b;
//                    break;
//                default:
//                    return "Unknown operation: " + operation;
//            }
//            return String.valueOf(result);
//        }
//
//        @Tool(name = "task_done", description = "Mark the task as completed")
//        public String taskDone(
//                @ToolParam(description = "Whether the task is done", required = false) Boolean done
//        ) {
//            return "{\"done\": " + (done != null && done) + "}";
//        }
//    }
//}