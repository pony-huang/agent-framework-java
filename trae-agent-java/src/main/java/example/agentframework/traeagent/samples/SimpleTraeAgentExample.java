package example.agentframework.traeagent.samples;

import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.providers.OpenAIChatClient;
import github.ponyhuang.agentframework.types.ChatResponse;
import example.agentframework.traeagent.TraeAgent;
import example.agentframework.traeagent.config.TraeAgentConfig;
import example.agentframework.traeagent.tools.TraeToolRegistry;
import example.agentframework.traeagent.trajectory.TrajectoryRecorder;

/**
 * Simple example showing how to use TraeAgent.
 *
 * This example demonstrates:
 * - Creating a TraeAgent with configuration
 * - Setting up tools
 * - Executing a simple task
 */
public class SimpleTraeAgentExample {

    public static void main(String[] args) {
        // Create configuration
        TraeAgentConfig config = new TraeAgentConfig();
        config.setProvider("openai");
        config.setBaseUrl(System.getenv("MY_OPENAI_BASE_URL"));
        config.setModel(System.getenv("MY_OPENAI_MODEL"));
        config.setApiKey(System.getenv("MY_OPENAI_API_KEY"));
        config.setMaxSteps(50);
        config.setWorkingDirectory(".");

        // Create chat client (Anthropic)
        ChatClient client = OpenAIChatClient.builder()
                .apiKey(config.getApiKey())
                .model(config.getModel())
                .baseUrl(config.getBaseUrl())
                .build();

        // Create tool registry
        TraeToolRegistry toolRegistry = new TraeToolRegistry(config);

        // Create trajectory recorder (optional)
        TrajectoryRecorder trajectoryRecorder = new TrajectoryRecorder(
                "./trajectory/trajectory.json", true);

        // Build the agent
        TraeAgent agent = TraeAgent.builder()
                .name("trae-agent")
                .client(client)
                .config(config)
                .toolExecutor(toolRegistry.getToolExecutor())
                .trajectoryRecorder(trajectoryRecorder)
                .build();

        // Execute a simple task
        String task = "Create a simple HelloWorld.java file in the current directory that prints 'Hello, World!'";

        System.out.println("Executing task: " + task);
        System.out.println("---");

        ChatResponse response = agent.newTask(task, java.util.Map.of(
                "project_path", config.getWorkingDirectory()
        ));

        // Print response
        if (response.getMessage() != null) {
            System.out.println(response.getMessage().getText());
        }

        // Check completion status
        System.out.println("---");
        if (agent.isTaskCompleted()) {
            System.out.println("Task completed!");
        } else {
            System.out.println("Task may not be complete");
        }
    }
}