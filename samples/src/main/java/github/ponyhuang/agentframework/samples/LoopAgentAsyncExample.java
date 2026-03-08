package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.LoopAgent;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.tools.ToolParam;
import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LoopAgentAsyncExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== LoopAgent Async Examples ===\n");

        CalculatorTool calculator = new CalculatorTool();
        ChatClient client = ClientExample.openAIChatClient();

        LoopAgent agent = LoopAgent.builder()
                .name("calculator-assistant-async")
                .instructions("You are a helpful assistant that uses the calculator tool for math operations.")
                .client(client)
                .tool(calculator)
                .maxSteps(5)
                .terminationHandler((fnName, fnArgs, result) -> "task_done".equals(fnName))
                .build();

        System.out.println("Example 1: Streaming Response with runStream");
        System.out.println("-------------------------------------------");
        runWithStream(agent);

        System.out.println("\nExample 2: Async Execution with CompletableFuture");
        System.out.println("------------------------------------------------");
        runWithCompletableFuture(agent);

        System.out.println("\nExample 3: Session with Async");
        System.out.println("-------------------------------");
        runSessionAsync(agent);
    }

    private static void runWithStream(LoopAgent agent) throws InterruptedException {
        List<Message> messages = List.of(
                UserMessage.create("Calculate 25 + 17, then multiply the result by 2")
        );

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        Flux<Message> stream = agent.runStream(messages);

        stream.subscribe(
                message -> {
                    if (message != null) {
                        String text = message.getTextContent();
                        if (text != null && !text.isBlank()) {
                            System.out.print("[Stream] " + text);
                            fullResponse.append(text);
                        }
                    }
                },
                error -> {
                    System.err.println("Stream error: " + error.getMessage());
                    latch.countDown();
                },
                () -> {
                    System.out.println("\n[Stream Complete]");
                    latch.countDown();
                }
        );

        if (!latch.await(60, TimeUnit.SECONDS)) {
            System.err.println("Stream timed out.");
        }
    }

    private static void runWithCompletableFuture(LoopAgent agent) throws InterruptedException {
        List<Message> messages = List.of(
                UserMessage.create("Calculate 50 + 30, then divide the result by 4")
        );

        CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() -> {
            return agent.run(messages);
        });

        ChatResponse response = future.join();

        System.out.println("=== Async Response ===");
        System.out.println(response.getMessage().getTextContent());
    }

    private static void runSessionAsync(LoopAgent agent) throws InterruptedException {
        System.out.println("Option 1: Using separate sessions for each request");
        System.out.println("---------------------------------------------------");

        var session1 = agent.createSession();
        CompletableFuture<ChatResponse> future1 = CompletableFuture.supplyAsync(() -> {
            return session1.run(UserMessage.create("What is 200 divided by 8?"));
        });
        ChatResponse response1 = future1.join();
        System.out.println("First question response: " + response1.getMessage().getTextContent());

        var session2 = agent.createSession();
        CompletableFuture<ChatResponse> future2 = CompletableFuture.supplyAsync(() -> {
            return session2.run(UserMessage.create("Now multiply that result by 3"));
        });
        ChatResponse response2 = future2.join();
        System.out.println("Follow-up response: " + response2.getMessage().getTextContent());

        System.out.println("\nOption 2: Using same session with history clearing");
        System.out.println("---------------------------------------------------");
        var session3 = agent.createSession();

        CompletableFuture<ChatResponse> f1 = CompletableFuture.supplyAsync(() -> {
            return session3.run(UserMessage.create("What is 100 divided by 4?"));
        });
        ChatResponse r1 = f1.join();
        System.out.println("Q1: " + r1.getMessage().getTextContent());

        session3.clearHistory();

        CompletableFuture<ChatResponse> f2 = CompletableFuture.supplyAsync(() -> {
            return session3.run(UserMessage.create("What is 50 multiplied by 6?"));
        });
        ChatResponse r2 = f2.join();
        System.out.println("Q2 (after clearing history): " + r2.getMessage().getTextContent());
    }

    public static class CalculatorTool {

        @Tool(name = "calculate", description = "Perform basic arithmetic calculations")
        public String calculate(
                @ToolParam(description = "The operation: add, subtract, multiply, or divide") String operation,
                @ToolParam(description = "First number") double a,
                @ToolParam(description = "Second number") double b
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
                @ToolParam(description = "Whether the task is done", required = false) Boolean done
        ) {
            return "{\"done\": " + (done != null && done) + "}";
        }
    }
}
