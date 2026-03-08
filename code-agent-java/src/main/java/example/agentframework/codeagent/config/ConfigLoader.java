package example.agentframework.codeagent.config;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Loads and parses YAML configuration for CodeAgent.
 * Supports environment variable overrides.
 */
public class ConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = "code-config.yaml";
    private static final String ENV_PREFIX = "CODE_";

    private final Yaml yaml;

    public ConfigLoader() {
        this.yaml = new Yaml();
    }

    /**
     * Load configuration from default location.
     */
    public AgentConfig load() throws IOException {
        return load(DEFAULT_CONFIG_FILE);
    }

    /**
     * Load configuration from specified file path.
     */
    public AgentConfig load(String configPath) throws IOException {
        Path path = Paths.get(configPath);
        if (!Files.exists(path)) {
            // Try current working directory
            path = Paths.get(System.getProperty("user.dir"), configPath);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Config file not found: " + configPath);
            }
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            AgentConfig config = yaml.loadAs(inputStream, AgentConfig.class);
            applyEnvironmentOverrides(config);
            return config;
        }
    }

    /**
     * Load configuration from input stream.
     */
    public AgentConfig load(InputStream inputStream) {
        AgentConfig config = yaml.loadAs(inputStream, AgentConfig.class);
        applyEnvironmentOverrides(config);
        return config;
    }

    /**
     * Apply environment variable overrides.
     * Supports: CODE_PROVIDER, CODE_MODEL, CODE_API_KEY, CODE_BASE_URL, CODE_MAX_STEPS, etc.
     */
    private void applyEnvironmentOverrides(AgentConfig config) {
        // Provider
        Optional.ofNullable(System.getenv(ENV_PREFIX + "PROVIDER"))
                .or(() -> Optional.ofNullable(System.getenv("ANTHROPIC_PROVIDER")))
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_PROVIDER")))
                .ifPresent(config::setProvider);

        // Model
        Optional.ofNullable(System.getenv(ENV_PREFIX + "MODEL"))
                .or(() -> Optional.ofNullable(System.getenv("ANTHROPIC_MODEL")))
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_MODEL")))
                .ifPresent(config::setModel);

        // API Key - multiple sources
        Optional.ofNullable(System.getenv(ENV_PREFIX + "API_KEY"))
                .or(() -> Optional.ofNullable(System.getenv("ANTHROPIC_API_KEY")))
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_API_KEY")))
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_KEY")))
                .ifPresent(config::setApiKey);

        // Base URL
        Optional.ofNullable(System.getenv(ENV_PREFIX + "BASE_URL"))
                .ifPresent(config::setBaseUrl);

        // Max Steps
        Optional.ofNullable(System.getenv(ENV_PREFIX + "MAX_STEPS"))
                .map(Integer::parseInt)
                .ifPresent(config::setMaxSteps);

        // Working Directory
        Optional.ofNullable(System.getenv(ENV_PREFIX + "WORKING_DIRECTORY"))
                .or(() -> Optional.ofNullable(System.getenv("WORKING_DIR")))
                .ifPresent(config::setWorkingDirectory);

        // Trajectory
        Optional.ofNullable(System.getenv(ENV_PREFIX + "TRAJECTORY_ENABLED"))
                .map(Boolean::parseBoolean)
                .ifPresent(config::setTrajectoryEnabled);

        Optional.ofNullable(System.getenv(ENV_PREFIX + "TRAJECTORY_PATH"))
                .ifPresent(config::setTrajectoryPath);
    }

    /**
     * Create configuration programmatically with required fields.
     */
    public static AgentConfig create(String provider, String model, String apiKey) {
        AgentConfig config = new AgentConfig();
        config.setProvider(provider);
        config.setModel(model);
        config.setApiKey(apiKey);
        return config;
    }

    /**
     * Create configuration with builder pattern.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AgentConfig config = new AgentConfig();

        public Builder provider(String provider) {
            config.setProvider(provider);
            return this;
        }

        public Builder model(String model) {
            config.setModel(model);
            return this;
        }

        public Builder apiKey(String apiKey) {
            config.setApiKey(apiKey);
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            config.setBaseUrl(baseUrl);
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            config.setMaxSteps(maxSteps);
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            config.setWorkingDirectory(workingDirectory);
            return this;
        }

        public Builder tools(String... tools) {
            config.setTools(java.util.Arrays.asList(tools));
            return this;
        }

        public Builder trajectoryEnabled(boolean enabled) {
            config.setTrajectoryEnabled(enabled);
            return this;
        }

        public Builder trajectoryPath(String path) {
            config.setTrajectoryPath(path);
            return this;
        }

        public AgentConfig build() {
            // Apply env overrides
            ConfigLoader loader = new ConfigLoader();
            loader.applyEnvironmentOverrides(config);
            return config;
        }
    }
}