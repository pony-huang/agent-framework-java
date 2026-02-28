package example.agentframework.traeagent.cli;

import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.providers.AnthropicChatClient;
import github.ponyhuang.agentframework.providers.OpenAIChatClient;
import github.ponyhuang.agentframework.types.ChatResponse;
import example.agentframework.traeagent.TraeAgent;
import example.agentframework.traeagent.config.ConfigLoader;
import example.agentframework.traeagent.config.TraeAgentConfig;
import example.agentframework.traeagent.tools.TraeToolRegistry;
import example.agentframework.traeagent.trajectory.TrajectoryRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * CLI for Trae Agent.
 */
@Command(
        name = "trae",
        description = "Trae Agent - LLM-powered software engineering assistant",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        subcommands = {
                TraeAgentCLI.RunCommand.class,
                TraeAgentCLI.InteractiveCommand.class,
                TraeAgentCLI.ShowConfigCommand.class
        }
)
public class TraeAgentCLI implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(TraeAgentCLI.class);

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TraeAgentCLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Run command - execute a single task.
     */
    @Command(name = "run", description = "Run a task")
    public static class RunCommand implements Callable<Integer> {

        @Option(names = {"-t", "--task"}, description = "Task to execute")
        private String task;

        @Option(names = {"-c", "--config"}, description = "Config file path")
        private String configPath = "trae-config.yaml";

        @Option(names = {"--working-dir"}, description = "Working directory")
        private String workingDir = ".";

        @Option(names = {"--provider"}, description = "LLM provider (anthropic, openai)")
        private String provider;

        @Option(names = {"--model"}, description = "Model name")
        private String model;

        @Option(names = {"--api-key"}, description = "API key")
        private String apiKey;

        @Option(names = {"--max-steps"}, description = "Maximum steps")
        private Integer maxSteps;

        @Option(names = {"--interactive"}, description = "Run in interactive mode")
        private boolean interactive;

        @Override
        public Integer call() {
            try {
                // Load configuration
                TraeAgentConfig config;
                try {
                    ConfigLoader loader = new ConfigLoader();
                    config = loader.load(configPath);
                } catch (FileNotFoundException e) {
                    LOG.info("Config file not found, using defaults");
                    config = new TraeAgentConfig();
                }

                // Override with CLI options
                if (workingDir != null && !workingDir.equals(".")) {
                    config.setWorkingDirectory(workingDir);
                }
                if (provider != null) {
                    config.setProvider(provider);
                }
                if (model != null) {
                    config.setModel(model);
                }
                if (apiKey != null) {
                    config.setApiKey(apiKey);
                }
                if (maxSteps != null) {
                    config.setMaxSteps(maxSteps);
                }

                // Validate config
                if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
                    LOG.error("API key not configured. Set it in config file or --api-key option.");
                    return 1;
                }

                // Create chat client
                ChatClient client = createChatClient(config);

                // Create tool registry
                TraeToolRegistry toolRegistry = new TraeToolRegistry(config);

                // Create trajectory recorder
                TrajectoryRecorder trajectoryRecorder = null;
                if (config.isTrajectoryEnabled()) {
                    Path trajPath = Paths.get(config.getTrajectoryPath(),
                            "trajectory-" + System.currentTimeMillis() + ".json");
                    trajectoryRecorder = new TrajectoryRecorder(trajPath.toString(), true);
                }

                // Build agent
                TraeAgent agent = TraeAgent.builder()
                        .name("trae-agent")
                        .client(client)
                        .config(config)
                        .toolExecutor(toolRegistry.getToolExecutor())
                        .trajectoryRecorder(trajectoryRecorder)
                        .build();

                if (interactive) {
                    return runInteractive(agent, config);
                }

                // Execute task
                if (task == null || task.isEmpty()) {
                    LOG.error("Task is required. Use --task option.");
                    return 1;
                }

                LOG.info("Executing task: {}", task);
                ChatResponse response = agent.newTask(task, java.util.Map.of(
                        "project_path", config.getWorkingDirectory()
                ));

                // Print response
                if (response.getMessage() != null) {
                    System.out.println("\n=== Response ===");
                    System.out.println(response.getMessage().getText());
                }

                // Print completion status
                if (agent.isTaskCompleted()) {
                    System.out.println("\n[OK] Task completed");
                } else {
                    System.out.println("\n[WARN] Task may not be complete");
                }

                return 0;

            } catch (Exception e) {
                LOG.error("Error executing task: {}", e.getMessage(), e);
                return 1;
            }
        }

        private ChatClient createChatClient(TraeAgentConfig config) {
            String provider = config.getProvider().toLowerCase();

            return switch (provider) {
                case "anthropic" -> AnthropicChatClient.builder()
                        .apiKey(config.getApiKey())
                        .model(config.getModel())
                        .baseUrl(config.getBaseUrl())
                        .build();
                case "openai" -> OpenAIChatClient.builder()
                        .apiKey(config.getApiKey())
                        .model(config.getModel())
                        .baseUrl(config.getBaseUrl())
                        .build();
                default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
            };
        }

        private int runInteractive(TraeAgent agent, TraeAgentConfig config) {
            java.util.Scanner scanner = new java.util.Scanner(System.in);

            System.out.println("=== Trae Agent Interactive Mode ===");
            System.out.println("Type 'exit' or 'quit' to exit\n");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    System.out.println("Goodbye!");
                    break;
                }

                try {
                    ChatResponse response = agent.newTask(input, java.util.Map.of(
                            "project_path", config.getWorkingDirectory()
                    ));

                    if (response.getMessage() != null) {
                        System.out.println("\n" + response.getMessage().getText() + "\n");
                    }

                    if (agent.isTaskCompleted()) {
                        System.out.println("[OK] Task completed\n");
                        agent.reset();
                    }

                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

            return 0;
        }
    }

    /**
     * Interactive command - run in interactive mode.
     */
    @Command(name = "interactive", description = "Run in interactive mode")
    public static class InteractiveCommand implements Callable<Integer> {

        @Option(names = {"-c", "--config"}, description = "Config file path")
        private String configPath = "trae-config.yaml";

        @Override
        public Integer call() {
            // This delegates to RunCommand with interactive flag
            RunCommand runCommand = new RunCommand();
            runCommand.configPath = configPath;
            runCommand.interactive = true;
            return runCommand.call();
        }
    }

    /**
     * Show config command - display current configuration.
     */
    @Command(name = "show-config", description = "Show current configuration")
    public static class ShowConfigCommand implements Callable<Integer> {

        @Option(names = {"-c", "--config"}, description = "Config file path")
        private String configPath = "trae-config.yaml";

        @Override
        public Integer call() {
            try {
                ConfigLoader loader = new ConfigLoader();
                TraeAgentConfig config = loader.load(configPath);

                System.out.println("=== Trae Agent Configuration ===\n");
                System.out.println("Provider: " + config.getProvider());
                System.out.println("Model: " + config.getModel());
                System.out.println("API Key: " + (config.getApiKey() != null && !config.getApiKey().isEmpty() ? "***" : "not set"));
                System.out.println("Base URL: " + (config.getBaseUrl() != null ? config.getBaseUrl() : "default"));
                System.out.println("Working Directory: " + config.getWorkingDirectory());
                System.out.println("Max Steps: " + config.getMaxSteps());
                System.out.println("Tools: " + config.getTools());
                System.out.println("Trajectory Enabled: " + config.isTrajectoryEnabled());
                System.out.println("Trajectory Path: " + config.getTrajectoryPath());

                return 0;

            } catch (FileNotFoundException e) {
                System.out.println("Config file not found: " + configPath);
                System.out.println("\nUsing defaults:");
                System.out.println("Provider: anthropic");
                System.out.println("Model: claude-sonnet-4-20250514");
                System.out.println("Max Steps: 100");
                return 0;
            } catch (Exception e) {
                LOG.error("Error loading config: {}", e.getMessage());
                return 1;
            }
        }
    }
}