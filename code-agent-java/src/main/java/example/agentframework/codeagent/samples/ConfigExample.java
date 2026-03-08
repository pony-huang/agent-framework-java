package example.agentframework.codeagent.samples;

import example.agentframework.codeagent.config.AgentConfig;
import example.agentframework.codeagent.config.ConfigLoader;

/**
 * Example showing how to load and use configuration.
 *
 * This example demonstrates:
 * - Loading configuration from YAML file
 * - Using the builder pattern
 * - Environment variable override
 */
public class ConfigExample {

    public static void main(String[] args) {
        System.out.println("=== Configuration Example ===\n");

        // Example 1: Load from YAML file
        System.out.println("1. Loading from YAML file:");
        try {
            // Try to load config (may fail if file doesn't exist)
            ConfigLoader loader = new ConfigLoader();
            AgentConfig config = loader.load("trae-config.yaml");
            printConfig(config);
        } catch (java.io.FileNotFoundException e) {
            System.out.println("Config file not found, using programmatic config");
            printProgrammaticConfig();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            printProgrammaticConfig();
        }

        // Example 2: Using builder pattern
        System.out.println("\n2. Using builder pattern:");
        AgentConfig builderConfig = ConfigLoader.builder()
                .provider("anthropic")
                .model("claude-sonnet-4-20250514")
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .maxSteps(100)
                .workingDirectory("/tmp")
                .tools("bash", "edit", "json_edit", "task_done")
                .trajectoryEnabled(true)
                .trajectoryPath("./trajectory")
                .build();

        printConfig(builderConfig);

        // Example 3: Environment variable overrides
        System.out.println("\n3. Environment variable support:");
        System.out.println("Supported environment variables:");
        System.out.println("  - CODE_PROVIDER / ANTHROPIC_PROVIDER / OPENAI_PROVIDER");
        System.out.println("  - CODE_MODEL / ANTHROPIC_MODEL / OPENAI_MODEL");
        System.out.println("  - CODE_API_KEY / ANTHROPIC_API_KEY / OPENAI_API_KEY");
        System.out.println("  - CODE_BASE_URL");
        System.out.println("  - CODE_MAX_STEPS");
        System.out.println("  - CODE_WORKING_DIRECTORY / WORKING_DIR");
        System.out.println("  - CODE_TRAJECTORY_ENABLED");
        System.out.println("  - CODE_TRAJECTORY_PATH");
    }

    private static void printConfig(AgentConfig config) {
        System.out.println("Provider: " + config.getProvider());
        System.out.println("Model: " + config.getModel());
        System.out.println("API Key: " + (config.getApiKey() != null && !config.getApiKey().isEmpty() ? "***" : "not set"));
        System.out.println("Max Steps: " + config.getMaxSteps());
        System.out.println("Working Directory: " + config.getWorkingDirectory());
        System.out.println("Tools: " + config.getTools());
        System.out.println("Trajectory Enabled: " + config.isTrajectoryEnabled());
    }

    private static void printProgrammaticConfig() {
        ConfigLoader loader = new ConfigLoader();
        AgentConfig config = ConfigLoader.create("anthropic", "claude-sonnet-4-20250514", "your-api-key");
        config.setMaxSteps(50);
        printConfig(config);
    }
}